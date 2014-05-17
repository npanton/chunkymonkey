package uk.co.badgersinfoil.chunkymonkey.ts;

import io.netty.buffer.ByteBuf;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.rtp.RtpConsumer;
import uk.co.badgersinfoil.chunkymonkey.rtp.RtpPacket;
import uk.co.badgersinfoil.chunkymonkey.rtp.RtpParser.RtpContext;
import uk.co.badgersinfoil.chunkymonkey.rtp.RtpParser.RtpLocator;

public class RtpTransportStreamParser implements RtpConsumer {

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

	public class RtpTransportStreamContext implements TSContext {
		private UdpConnectionLocator connLocator;
		public TSContext consumerContext;
	}

	private TSPacketConsumer consumer;

	public RtpTransportStreamParser(TSPacketConsumer consumer) {
		this.consumer = consumer;
	}


	@Override
	public void packet(TSContext c, RtpPacket p) {
		RtpTransportStreamContext ctx = (RtpTransportStreamContext)c;
		ByteBuf payload = p.payload();
		int count = payload.readableBytes() / TSPacket.TS_PACKET_LENGTH;
		RtpLocator locator = new RtpLocator(p, ctx.connLocator);
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

	@Override
	public TSContext createContext(RtpContext ctx) {
		RtpTransportStreamContext rtptsCtx = new RtpTransportStreamContext();
		rtptsCtx.connLocator = new UdpConnectionLocator(5004);
		rtptsCtx.consumerContext = consumer.createContext(ctx);
		return rtptsCtx;
	}
}
