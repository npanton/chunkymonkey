package uk.co.badgersinfoil.chunkymonkey.ts;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;

import uk.co.badgersinfoil.chunkymonkey.Locator;

public class RtpTransportStreamParser {
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

	private TSPacketConsumer consumer;
	private UdpConnectionLocator connLocator;

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
		connLocator = new UdpConnectionLocator(5004);
	}

	public void recieve() throws IOException {
		SocketAddress local = new InetSocketAddress(5004);
		DatagramChannel ch = DatagramChannel.open(StandardProtocolFamily.INET).bind(local);
		ByteBuffer dst = ByteBuffer.allocateDirect(1500);
		while (true) {
			SocketAddress peer = ch.receive(dst);
			if (peer == null) {
				System.out.println("nothing received");
			} else {
				dst.flip();
				ByteBuf buf = Unpooled.wrappedBuffer(dst);
				packet(buf);
			}
		}
	}

	public void packet(ByteBuf buf) {
		RtpPacket p = new RtpPacket(buf);
		ByteBuf payload = p.payload();
		int count = payload.readableBytes() / TSPacket.TS_PACKET_LENGTH;
		RtpPacketLocator locator = new RtpPacketLocator(connLocator, p.timestamp(), p.sequenceNumber());
		for (int i=0; i<count; i++) {
			ByteBuf pk = payload.slice(i*TSPacket.TS_PACKET_LENGTH, TSPacket.TS_PACKET_LENGTH);
			TSPacket packet = new TSPacket(locator, i, pk);
			if (!packet.synced()) {
				// TODO: better diagnostics.  re-sync?
				throw new RuntimeException("Transport stream synchronisation lost @packet#"+i+" in "+locator);
			}
			consumer.packet(null, packet);
		}
	}
}
