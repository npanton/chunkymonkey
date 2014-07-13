package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class SeqParamSetNalUnitConsumer implements NalUnitConsumer {

	public static class SeqParamSetNalUnitContext implements NalUnitContext {

		private H264Context ctx;
		ByteBuf seqParamSetBuffer = Unpooled.buffer();
		private MediaContext consumerContext;

		public SeqParamSetNalUnitContext(H264Context ctx) {
			this.ctx = ctx;
		}
		@Override
		public H264Context getH264Context() {
			return ctx;
		}
		@Override
		public Locator getLocator() {
			return ctx.getLocator();
		}
		public void setConsumerContext(MediaContext consumerContext) {
			this.consumerContext = consumerContext;
		}
		public MediaContext getConsumerContext() {
			return consumerContext;
		}
	}

	private SeqParamSetConsumer consumer = SeqParamSetConsumer.NULL;

	public void setConsumer(SeqParamSetConsumer consumer) {
		this.consumer = consumer;
	}

	@Override
	public void start(NalUnitContext ctx, NALUnit u) {
	}
	@Override
	public void data(NalUnitContext ctx, ByteBuf buf, int offset, int length) {
		SeqParamSetNalUnitContext sctx = (SeqParamSetNalUnitContext)ctx;
		sctx.seqParamSetBuffer.writeBytes(buf, offset, length);
	}
	@Override
	public void end(NalUnitContext ctx) {
		SeqParamSetNalUnitContext sctx = (SeqParamSetNalUnitContext)ctx;
		ByteBuf buf = sctx.seqParamSetBuffer;
		SeqParamSet params = new SeqParamSet(buf);
		consumer.seqParamSet(sctx.getConsumerContext(), params);
		sctx.getH264Context().lastSeqParamSet(params);
		buf.clear();
	}
	@Override
	public void continuityError(NalUnitContext ctx) {
		SeqParamSetNalUnitContext sctx = (SeqParamSetNalUnitContext)ctx;
		ByteBuf buf = sctx.seqParamSetBuffer;
		buf.clear();
	}
	@Override
	public NalUnitContext createContext(H264Context ctx) {
		SeqParamSetNalUnitContext spsCtx = new SeqParamSetNalUnitContext(ctx);
		spsCtx.setConsumerContext(consumer.createContext(spsCtx));
		return spsCtx;
	}
}
