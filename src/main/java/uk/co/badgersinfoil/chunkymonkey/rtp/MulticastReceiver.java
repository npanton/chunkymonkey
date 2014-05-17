package uk.co.badgersinfoil.chunkymonkey.rtp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import uk.co.badgersinfoil.chunkymonkey.rtp.RtpParser.RtpContext;

public class MulticastReceiver {
	public class MulticastReceiverContext {

		private RtpContext rtpCtx;

		public MulticastReceiverContext(RtpContext rtpCtx) {
			this.rtpCtx = rtpCtx;
		}

	}

	private static final int MAX_DATAGRAM_LENGTH = 1500;
	private MulticastSocket ms;
	private RtpParser parser;

	public MulticastReceiver(RtpParser parser, String multicastGroup, NetworkInterface interf) throws IOException {
		this.parser = parser;
		ms = new MulticastSocket(5004);
		InetAddress group = InetAddress.getByName(multicastGroup);
		ms.joinGroup(group);
		ms.setNetworkInterface(interf);
	}

	public void receive(MulticastReceiverContext ctx) throws IOException {
		while (true) {
			ByteBuf buf = Unpooled.buffer(MAX_DATAGRAM_LENGTH);
			DatagramPacket p = new DatagramPacket(buf.array() , MAX_DATAGRAM_LENGTH);
			ms.receive(p);
			buf.writerIndex(p.getLength());
			parser.packet(ctx.rtpCtx, buf);
		}
	}

	public MulticastReceiverContext createContext() {
		return new MulticastReceiverContext(parser.createContext());
	}
}
