package uk.co.badgersinfoil.chunkymonkey.rtp;

import io.netty.buffer.ByteBuf;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.ts.RtpTransportStreamParser.UdpConnectionLocator;
import uk.co.badgersinfoil.chunkymonkey.ts.TSContext;

public class RtpParser {
	public static class RtpLocator implements Locator {

		private Locator parent;
		private RtpPacket packet;

		public RtpLocator(RtpPacket p, Locator parent) {
			this.parent = parent;
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

	public static class RtpContext implements TSContext {
		private UdpConnectionLocator connLocator;
		private long ssrc = -1;
		private int lastSeq;
		private long lastTimestamp = -1;
		public TSContext consumerContext;
	}

	private RtpConsumer consumer;
	private RTPErrorHandler err = RTPErrorHandler.NULL;

	public RtpParser(RtpConsumer consumer) {
		this.consumer = consumer;
	}

	public void packet(RtpContext ctx, ByteBuf buf) {
		RtpPacket p = new RtpPacket(buf);
		check(ctx, p);
		consumer.packet(ctx.consumerContext, p);
	}

	public void setRTPErrorHandler(RTPErrorHandler err) {
		this.err = err;
	}

	private void check(RtpContext ctx, RtpPacket p) {
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

	public RtpContext createContext() {
		RtpContext ctx = new RtpContext();
		ctx.connLocator = new UdpConnectionLocator(5004);
		ctx.consumerContext = consumer.createContext(ctx);
		return ctx;
	}
}
