package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PicParamSetNalUnitConsumer implements NalUnitConsumer {

	public class PicParamSetNalUnitContext implements NalUnitContext {

		private H264Context ctx;
		public ByteBuf picParamSetBuffer = Unpooled.buffer();

		public PicParamSetNalUnitContext(H264Context ctx) {
			this.ctx = ctx;
		}

		@Override
		public Locator getLocator() {
			return ctx.getLocator();
		}
		@Override
		public H264Context getH264Context() {
			return ctx;
		}
	}

	private Reporter rep = Reporter.NULL;

	public void setReporder(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void start(NalUnitContext ctx, NALUnit u) {
	}

	@Override
	public void data(NalUnitContext ctx, ByteBuf buf, int offset, int length) {
		PicParamSetNalUnitContext pctx = (PicParamSetNalUnitContext)ctx;
		pctx.picParamSetBuffer.writeBytes(buf, offset, length);
	}

	@Override
	public void end(NalUnitContext ctx) {
		PicParamSetNalUnitContext pctx = (PicParamSetNalUnitContext)ctx;
		ByteBuf buf = pctx.picParamSetBuffer;
		PicParamSet params = PicParamSet.parse(pctx, buf, rep);
		pctx.getH264Context().lastPicParamSet(params);
		buf.clear();
	}

	@Override
	public void continuityError(NalUnitContext ctx) {
	}

	@Override
	public NalUnitContext createContext(H264Context ctx) {
		return new PicParamSetNalUnitContext(ctx);
	}
}
