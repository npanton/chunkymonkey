package uk.co.badgersinfoil.chunkymonkey.h264;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class SeqParamSetNalUnitConsumer implements NalUnitConsumer {

	@Override
	public void unit(H264Context ctx, NALUnit u) {
		ByteBuf buf = u.getContent();
		SeqParamSet params = new SeqParamSet(buf);
		ctx.lastSeqParamSet(params);
	}
}
