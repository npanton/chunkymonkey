package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.h264.NalUnitConsumer.NalUnitContext;
import uk.co.badgersinfoil.chunkymonkey.h264.PicTimingConsumer.PicTimingContext;
import uk.co.badgersinfoil.chunkymonkey.h264.SeqParamSet.VuiParameters;

public class PicTimingSeiConsumer implements SeiHeaderConsumer {

	public class PicTimingSeiContext implements SeiHeaderContext {
		private NalUnitContext ctx;
		private PicTimingContext consumerContext;

		public PicTimingSeiContext(NalUnitContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public NalUnitContext getNalUnitContext() {
			return ctx;
		}

		public void setConsumerContext(PicTimingContext consumerContext) {
			this.consumerContext = consumerContext;
		}
	}

	private PicTimingConsumer consumer;

	public PicTimingSeiConsumer(PicTimingConsumer consumer) {
		this.consumer = consumer;
	}

	@Override
	public void header(SeiHeaderContext ctx, SeiHeader seiHeader) {
		SeqParamSet seqParamSet = ctx.getNalUnitContext().getH264Context().lastSeqParamSet();
		if (seqParamSet != null && seqParamSet.vuiParameters() != null) {
			VuiParameters vuiParameters = seqParamSet.vuiParameters();
			PicTimingHeader picTiming
				= new PicTimingHeader(seiHeader.getBuf(),
						      vuiParameters);
			consumer.picTiming(((PicTimingSeiContext)ctx).consumerContext, picTiming);
		}
	}
	@Override
	public SeiHeaderContext createContext(NalUnitContext ctx) {
		PicTimingSeiContext picTimingSeiCtx = new PicTimingSeiContext(ctx);
		picTimingSeiCtx.setConsumerContext(consumer.createContext(picTimingSeiCtx));
		return picTimingSeiCtx;
	}
}
