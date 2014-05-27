package uk.co.badgersinfoil.chunkymonkey.ts;

import io.netty.buffer.ByteBuf;
import uk.co.badgersinfoil.chunkymonkey.Locator;

/**
 * Common code for handlers of buffers containing multiple transport stream
 * packets.
 */
public class BufferTransportStreamParser {
	public static class BufferContext implements TSContext {

		public TSContext consumerContext;
		private TSContext parent;

		public BufferContext(TSContext parent) {
			this.parent = parent;
		}
	}

	private TSPacketConsumer consumer;

	public BufferTransportStreamParser(TSPacketConsumer consumer) {
		this.consumer = consumer;
	}

	/**
	 * Parses a series of TSPacket objects from the given buffer (which
	 * the caller should take care is an exact multiple of
	 * {@link TSPacket#TS_PACKET_LENGTH} bytes long), passing each to the
	 * TSPacketConsumer instance supplied on construction.
	 */
	public void buffer(TSContext c, ByteBuf payload, Locator locator) {
		BufferContext ctx = (BufferContext)c;
		int count = payload.readableBytes() / TSPacket.TS_PACKET_LENGTH;
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

	public TSContext createContext(TSContext ctx) {
		BufferContext bufCtx = new BufferContext(ctx);
		bufCtx.consumerContext = consumer.createContext(bufCtx);
		return bufCtx;
	}

//	public void discontinuity(TSContext c) {
//		BufferContext ctx = (BufferContext)c;
//		consumer.discontinuity(ctx.consumerContext);
//	}
}
