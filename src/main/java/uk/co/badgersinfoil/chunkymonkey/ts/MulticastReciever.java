package uk.co.badgersinfoil.chunkymonkey.ts;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import uk.co.badgersinfoil.chunkymonkey.ts.RtpTransportStreamParser.RtpTransportStreamContext;

public class MulticastReciever {
	public class MulticastRecieverContext {

		private RtpTransportStreamContext rtpCtx;

		public MulticastRecieverContext(RtpTransportStreamContext rtpCtx) {
			this.rtpCtx = rtpCtx;
		}

	}

	private static final int MAX_DATAGRAM_LENGTH = 1500;
	private MulticastSocket ms;
	private RtpTransportStreamParser parser;

	public MulticastReciever(RtpTransportStreamParser parser, String multicastGroup, NetworkInterface interf) throws IOException {
		this.parser = parser;
		ms = new MulticastSocket(5004);
		InetAddress group = InetAddress.getByName(multicastGroup);
		ms.joinGroup(group);
		ms.setNetworkInterface(interf);
	}

	public void recieve(MulticastRecieverContext ctx) throws IOException {
		while (true) {
			ByteBuf buf = Unpooled.buffer(MAX_DATAGRAM_LENGTH);
			DatagramPacket p = new DatagramPacket(buf.array() , MAX_DATAGRAM_LENGTH);
			ms.receive(p);
			buf.writerIndex(p.getLength());
			parser.packet(ctx.rtpCtx, buf);
		}
	}

	public MulticastRecieverContext createContext() {
		return new MulticastRecieverContext(parser.createContext());
	}
}
