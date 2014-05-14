package uk.co.badgersinfoil.chunkymonkey.ts;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import uk.co.badgersinfoil.chunkymonkey.Locator;

public class RtpTransportStreamParser {

	public class RtpLocator implements Locator {

		private UdpConnectionLocator parent;
		private RtpPacket packet;

		public RtpLocator(RtpPacket p, UdpConnectionLocator connLocator) {
			parent = connLocator;
			packet = p;
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			b.append("RTP Packet seq=").append(packet.sequenceNumber())
			 .append(" ts=").append(packet.timestamp())
			 .append(" ssrc=").append(packet.ssrc());
			Locator parent = getParent();
			if (parent != null) {
				b.append("\n  at ");
				b.append(parent);
			}
			return b.toString();
		}

		@Override
		public Locator getParent() {
			return parent;
		}

	}

	public static class UdpConnectionLocator implements Locator {

		private int port;
		private Locator parent;

		public UdpConnectionLocator(int port) {
			this.port = port;
		}
		@Override
		public String toString() {
			return "UDP connection on port "+port;
		}

		@Override
		public Locator getParent() {
			return parent;
		}

	}

	public static class RtpPacketLocator implements Locator {
		private Locator parent;
		private long timestamp;
		private int sequenceNumber;

		public RtpPacketLocator(Locator parent, long timestamp, int sequenceNumber) {
			this.parent = parent;
			this.timestamp = timestamp;
			this.sequenceNumber = sequenceNumber;
		}

		@Override
		public String toString() {
			return "RTP packet timestamp="+timestamp+" seq="+sequenceNumber+"\n  at "+parent;
		}

		@Override
		public Locator getParent() {
			return parent;
		}
	}

	public class RtpTransportStreamContext implements TSContext {
		private UdpConnectionLocator connLocator;
		private long ssrc = -1;
		private int lastSeq;
		private long lastTimestamp = -1;
		public TSContext consumerContext;
	}

	private TSPacketConsumer consumer;
	private RTPErrorHandler err = RTPErrorHandler.NULL;

	public static class RtpPacket {

		private ByteBuf buf;

		public RtpPacket(ByteBuf buf) {
			this.buf = buf;
		}

		public int version() {
			return (buf.getByte(0) & 0b11000000) >> 6;
		}

		public boolean padding() {
			return (buf.getByte(0) & 0b00100000) != 0;
		}

		public boolean extension() {
			return (buf.getByte(0) & 0b00010000) != 0;
		}

		public int csrcCount() {
			return buf.getByte(0) & 0b00001111;
		}

		public boolean mark() {
			return (buf.getByte(1) & 0b10000000) != 0;
		}

		public int payloadType() {
			return buf.getByte(1) & 0b011111111;
		}

		public int sequenceNumber() {
			return buf.getUnsignedShort(2);
		}

		public long timestamp() {
			return buf.getUnsignedInt(4);
		}

		public long ssrc() {
			return buf.getUnsignedInt(8);
		}

		public int[] csrcs() {
			int c = csrcCount();
			int[] result = new int[c];
			for (int i=0; i<c; i++) {
				result[i] = buf.getInt(12 + 4*i);
			}
			return result;
		}

		private int payloadOffset() {
			int offset = 12 + 4 * csrcCount();
			if (extension()) {
				int len = buf.getUnsignedShort(offset+2);
				offset += 4 + len;
			}
			return offset;
		}

		public ByteBuf payload() {
			int off = payloadOffset();
			return buf.slice(off, buf.readableBytes() - off);
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			b.append("version=").append(version())
			 .append(" padding=").append(padding())
			 .append(" extension=").append(extension())
			 .append(" csrcCount=").append(csrcCount())
			 .append(" mark=").append(mark())
			 .append(" payloadType=").append(payloadType())
			 .append(" sequenceNumber=").append(sequenceNumber())
			 .append(" timestamp=").append(timestamp())
			 .append(" ssrc=").append(ssrc());
			if (csrcCount() > 0) {
				b.append("csrcs=")
				 .append(Arrays.toString(csrcs()));
			}
			return b.toString();
		}
	}

	public RtpTransportStreamParser(TSPacketConsumer consumer) {
		this.consumer = consumer;
	}

	public void setRTPErrorHandler(RTPErrorHandler err) {
		this.err = err;
	}

	public void packet(RtpTransportStreamContext ctx, ByteBuf buf) {
		RtpPacket p = new RtpPacket(buf);
		check(ctx, p);
		ByteBuf payload = p.payload();
		int count = payload.readableBytes() / TSPacket.TS_PACKET_LENGTH;
		RtpPacketLocator locator = new RtpPacketLocator(ctx.connLocator, p.timestamp(), p.sequenceNumber());
		for (int i=0; i<count; i++) {
			ByteBuf pk = payload.slice(i*TSPacket.TS_PACKET_LENGTH, TSPacket.TS_PACKET_LENGTH);
			TSPacket packet = new TSPacket(locator, i, pk);
			if (!packet.synced()) {
				// TODO: better diagnostics.  re-sync?
				throw new RuntimeException("Transport stream synchronisation lost @packet#"+i+" in "+locator);
			}
			consumer.packet(ctx.consumerContext, packet);
		}
	}

	private void check(RtpTransportStreamContext ctx, RtpPacket p) {
		RtpLocator loc = new RtpLocator(p, ctx.connLocator);
		if (ctx.ssrc == -1) {
			ctx.ssrc = p.ssrc();
		} else if (ctx.ssrc != p.ssrc()) {
			err.unexpectedSsrc(loc, ctx.ssrc, p.ssrc());
		}
		if (ctx.lastSeq != -1) {
			int expected = nextSeq(ctx.lastSeq);
			if (expected != p.sequenceNumber()) {
				err.unexpectedSequenceNumber(loc, expected, p.sequenceNumber());
			}
		}
		ctx.lastSeq = p.sequenceNumber();
		if (ctx.lastTimestamp != -1) {
			// TODO: are there some tighter general constraints we
			// can place on allowed ts differences?
			long diff = p.timestamp() - ctx.lastTimestamp;
			if (willWrapSoon(ctx.lastTimestamp) && diff < 0) {
				diff += 0x100000000L;
			}
			if (diff < 0) {
				err.timeWentBackwards(loc, ctx.lastTimestamp, p.timestamp());
			} else if (diff > 0x100000000L / 4) {
				err.timestampJumped(loc, ctx.lastTimestamp, p.timestamp());
			}
		}
		ctx.lastTimestamp = p.timestamp();
	}

	private boolean willWrapSoon(long ts) {
		// are we more than three quarters through the set of available
		// timestamp counter values?
		return ts > 0x100000000L * 3 / 4;
	}

	private int nextSeq(int seq) {
		return (seq + 1) & 0xffff;
	}

	public RtpTransportStreamContext createContext() {
		RtpTransportStreamContext ctx = new RtpTransportStreamContext();
		ctx.connLocator = new UdpConnectionLocator(5004);
		ctx.consumerContext = consumer.createContext(ctx);
		return ctx;
	}
}
