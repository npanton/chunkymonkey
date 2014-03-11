package uk.co.badgersinfoil.chunkymonkey.h264;

import io.netty.buffer.ByteBuf;

public class SeqParamSetNalUnitConsumer implements NalUnitConsumer {

	@Override
	public void start(H264Context ctx, NALUnit u) {
	}
	@Override
	public void data(H264Context ctx, ByteBuf buf, int offset, int length) {
		ctx.seqParamSetBuffer().writeBytes(buf, offset, length);
	}
	@Override
	public void end(H264Context ctx) {
		ByteBuf buf = ctx.seqParamSetBuffer();
		SeqParamSet params = new SeqParamSet(buf);
		ctx.lastSeqParamSet(params);
		buf.clear();
	}
}
