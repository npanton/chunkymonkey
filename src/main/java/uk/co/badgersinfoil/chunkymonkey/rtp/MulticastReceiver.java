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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.rtp.RtpParser.RtpContext;

public class MulticastReceiver {
	public class MulticastReceiverContext {

		private DatagramChannel rtpChannel;
		public DatagramChannel fecChannel;
		public Selector selector;
		private RtpContext rtpCtx;
		public SelectionKey rtpKey;
		public SelectionKey fecKey;

		public MulticastReceiverContext(RtpContext rtpCtx) {
			this.rtpCtx = rtpCtx;
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

	private static final int MAX_DATAGRAM_LENGTH = 1500;
	private RtpParser parser;

	// TODO: This flag is a bit of a hack.  Use strategy pattern or something.
	private boolean doFec;

	public MulticastReceiver(RtpParser rtpParser, boolean doFec) {
		this.parser = rtpParser;
		this.doFec = doFec;
	}

	public void receive(MulticastReceiverContext ctx) throws IOException {
		while (true) {
			int num = ctx.selector.select();
			if (num > 0) {
				Set<SelectionKey> keys = ctx.selector.selectedKeys();
				if (keys.contains(ctx.rtpKey)) {
					ByteBuffer b = ByteBuffer.allocate(MAX_DATAGRAM_LENGTH);
					if (ctx.rtpChannel.receive(b) == null) {
						break;
					}
					b.flip();
					ByteBuf buf = Unpooled.wrappedBuffer(b);
					parser.packet(ctx.rtpCtx, buf);
				}
				if (doFec && keys.contains(ctx.fecKey)) {
					while (true) {
						ByteBuffer b = ByteBuffer.allocate(MAX_DATAGRAM_LENGTH);
						if (ctx.fecChannel.receive(b) == null) {
							break;
						}
						b.flip();
						ByteBuf buf = Unpooled.wrappedBuffer(b);
						parser.fecPacket(ctx.rtpCtx, buf);
					}
				}
				keys.clear();
			}
		}
	}

	public MulticastReceiverContext createContext(String multicastGroup, NetworkInterface interf) {
		int port = 5004;
		UdpConnectionLocator loc = new UdpConnectionLocator(port);
		MulticastReceiverContext ctx = new MulticastReceiverContext(parser.createContext(null, loc));
		try {
			ctx.selector = Selector.open();
			InetAddress group = InetAddress.getByName(multicastGroup);
			ctx.rtpChannel = createChannel(port);
			ctx.rtpChannel.configureBlocking(false);
			ctx.rtpChannel.join(group, interf);
			ctx.rtpKey = ctx.rtpChannel.register(ctx.selector, SelectionKey.OP_READ);
			if (doFec) {
				ctx.fecChannel = createChannel(port+2);
				ctx.fecChannel.configureBlocking(false);
				ctx.fecChannel.join(group, interf);
				ctx.fecKey = ctx.fecChannel.register(ctx.selector, SelectionKey.OP_READ);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return ctx;
	}

	private DatagramChannel createChannel(int port) throws IOException {
		return DatagramChannel
			.open(StandardProtocolFamily.INET)
			.setOption(StandardSocketOptions.SO_REUSEADDR, true)
			.bind(new InetSocketAddress(port));
	}
}
