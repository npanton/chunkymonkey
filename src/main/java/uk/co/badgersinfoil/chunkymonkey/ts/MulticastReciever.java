package uk.co.badgersinfoil.chunkymonkey.ts;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class MulticastReciever {

	private DatagramChannel ch;

	public MulticastReciever(String multicastGroup, NetworkInterface interf) throws IOException {
		SocketAddress local = new InetSocketAddress(5004);
		ch = DatagramChannel.open(StandardProtocolFamily.INET).bind(local);
		InetAddress group = InetAddress.getByName(multicastGroup);
		ch.join(group , interf);
	}

	public void recieve(RtpTransportStreamParser parser) throws IOException {
		ByteBuffer dst = ByteBuffer.allocateDirect(1500);
		while (true) {
			SocketAddress peer = ch.receive(dst);
			dst.flip();
			ByteBuf buf = Unpooled.wrappedBuffer(dst);
			parser.packet(peer, buf);
		}
	}
}
