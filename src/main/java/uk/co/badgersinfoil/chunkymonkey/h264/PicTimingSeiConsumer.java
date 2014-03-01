package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.h264.SeqParamSet.VuiParameters;

public class PicTimingSeiConsumer implements SeiHeaderConsumer {

	private PicTimingConsumer consumer;

	public PicTimingSeiConsumer(PicTimingConsumer consumer) {
		this.consumer = consumer;
	}

	@Override
	public void header(H264Context ctx, SeiHeader seiHeader) {
		SeqParamSet seqParamSet = ctx.lastSeqParamSet();
		if (seqParamSet != null && seqParamSet.vuiParameters() != null) {
			VuiParameters vuiParameters = seqParamSet.vuiParameters();
			PicTimingHeader picTiming
				= new PicTimingHeader(seiHeader.getBuf(),
						      vuiParameters);
			consumer.picTiming(ctx, picTiming);
		}
	}

}
