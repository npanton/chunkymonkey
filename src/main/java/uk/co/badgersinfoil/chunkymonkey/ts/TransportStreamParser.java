package uk.co.badgersinfoil.chunkymonkey.ts;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.InputStream;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;


public class TransportStreamParser {

	public static class TransportStreamParserContext implements MediaContext {

		private MediaContext consumerContext;
		private MediaContext parentContext;
		private int packetNo;

		public TransportStreamParserContext(MediaContext parentContext) {
			this.parentContext = parentContext;
		}

		@Override
		public Locator getLocator() {
			return new TSPacketLocator(parentContext.getLocator(), packetNo);
		}

		public void setConsumerContext(MediaContext consumerContext) {
			this.consumerContext = consumerContext;
		}

		public MediaContext getParent() {
			return parentContext;
		}
	}

	private TSPacketConsumer consumer;

	public TransportStreamParser(TSPacketConsumer consumer) {
		this.consumer = consumer;
	}

	public void parse(MediaContext ctx, InputStream stream) throws IOException {
		TransportStreamParserContext pctx = (TransportStreamParserContext)ctx;
		while (true) {
			ByteBuf buf = Unpooled.buffer();
			if (!readPacket(stream, buf, TSPacket.TS_PACKET_LENGTH)) {
				break;
			}
			ByteBuf pk = buf.slice(buf.readerIndex(), TSPacket.TS_PACKET_LENGTH);
			TSPacket packet = new TSPacket(pk);
			if (!packet.synced()) {
				// TODO: better diagnostics.  re-sync?
				throw new RuntimeException("Transport stream synchronisation lost @packet#"+pctx.packetNo+" in "+pctx.getLocator());
			}
			consumer.packet(pctx.consumerContext, packet);
			pctx.packetNo++;
		}
		consumer.end(pctx.consumerContext);
	}

	private boolean readPacket(InputStream in, ByteBuf buf, int packetSize) throws IOException {
		int read = 0;
		while (read < packetSize) {
			int r = buf.writeBytes(in, packetSize - read);
			if (r == -1) {
				if (read > 0 && read < packetSize) {
					throw new RuntimeException("Wanted "+packetSize+" bytes, but only "+read+" read");
				}
				return false;
			}
			read += r;
		}
		return true;
	}

	public MediaContext createContext(MediaContext parentCtx) {
		TransportStreamParserContext ctx = new TransportStreamParserContext(parentCtx);
		ctx.setConsumerContext(consumer.createContext(ctx));
		return ctx;
	}
}
