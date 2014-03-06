package uk.co.badgersinfoil.chunkymonkey.h264;

import io.netty.buffer.ByteBuf;
import uk.co.badgersinfoil.chunkymonkey.Reporter;

public class ValidatingNalUnitConsumer implements NalUnitConsumer {

	private Reporter rep;

	public ValidatingNalUnitConsumer(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void start(H264Context ctx, NALUnit u) {
	}
	@Override
	public void data(H264Context ctx, ByteBuf buf) {
	}
	@Override
	public void end(H264Context ctx) {
	}
}
