package uk.co.badgersinfoil.chunkymonkey.ts;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

public class MulticastReciever {
	private static final int MAX_DATAGRAM_LENGTH = 1500;
	private MulticastSocket ms;
	
	public MulticastReciever(String multicastGroup, NetworkInterface interf) throws IOException {
		ms = new MulticastSocket(5004);
		InetAddress group = InetAddress.getByName(multicastGroup);
		ms.joinGroup(group);
		ms.setNetworkInterface(interf);
	}

	public void recieve(RtpTransportStreamParser parser) throws IOException {
		while (true) {
			ByteBuf buf = Unpooled.buffer(MAX_DATAGRAM_LENGTH);
			DatagramPacket p = new DatagramPacket(buf.array() , MAX_DATAGRAM_LENGTH);
			ms.receive(p);
			buf.writerIndex(p.getLength());
			parser.packet(buf);
		}
	}
}
