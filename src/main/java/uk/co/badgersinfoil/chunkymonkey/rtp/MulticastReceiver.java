package uk.co.badgersinfoil.chunkymonkey.rtp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import uk.co.badgersinfoil.chunkymonkey.rtp.RtpParser.RtpContext;

public class MulticastReceiver {
	public class MulticastReceiverContext {

		private RtpContext rtpCtx;

		public MulticastReceiverContext(RtpContext rtpCtx) {
			this.rtpCtx = rtpCtx;
		}

	}

	private static final int MAX_DATAGRAM_LENGTH = 1500;
	private RtpParser parser;
	private DatagramChannel dc;

	public MulticastReceiver(RtpParser parser, String multicastGroup, NetworkInterface interf) throws IOException {
		this.parser = parser;
		dc = DatagramChannel
			.open(StandardProtocolFamily.INET)
			.setOption(StandardSocketOptions.SO_REUSEADDR, true)
			.bind(new InetSocketAddress(5004));
		dc.join(InetAddress.getByName(multicastGroup), interf);
	}

	public void receive(MulticastReceiverContext ctx) throws IOException {
		while (true) {
			ByteBuffer b = ByteBuffer.allocate(MAX_DATAGRAM_LENGTH);
			dc.receive(b);
			b.flip();
			ByteBuf buf = Unpooled.wrappedBuffer(b);
			parser.packet(ctx.rtpCtx, buf);
		}
	}

	public MulticastReceiverContext createContext() {
		return new MulticastReceiverContext(parser.createContext());
	}
}
