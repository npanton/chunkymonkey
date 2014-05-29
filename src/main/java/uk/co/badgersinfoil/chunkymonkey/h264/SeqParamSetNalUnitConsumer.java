package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.Locator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class SeqParamSetNalUnitConsumer implements NalUnitConsumer {

	public class SeqParamSetNalUnitContext implements NalUnitContext {

		private H264Context ctx;
		ByteBuf seqParamSetBuffer = Unpooled.buffer();

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
		return new SeqParamSetNalUnitContext(ctx);
	}
}
