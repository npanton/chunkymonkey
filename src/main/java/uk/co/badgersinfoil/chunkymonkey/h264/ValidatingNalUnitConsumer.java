package uk.co.badgersinfoil.chunkymonkey.h264;

import io.netty.buffer.ByteBuf;
import uk.co.badgersinfoil.chunkymonkey.Reporter;

public class ValidatingNalUnitConsumer implements NalUnitConsumer {

	private Reporter rep;

	public ValidatingNalUnitConsumer(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void start(NalUnitContext ctx, NALUnit u) {
		if (u.forbiddenZeroBit() != 0) {
			rep.carp(u.getLocator(), "forbidden_zero_bit should always be 0, found: "+u.forbiddenZeroBit());
		}
	}
	@Override
	public void data(NalUnitContext ctx, ByteBuf buf, int offset, int length) {
	}
	@Override
	public void end(NalUnitContext ctx) {
	}

	@Override
	public void continuityError(NalUnitContext ctx) {
	}

	@Override
	public NalUnitContext createContext(final H264Context ctx) {
		return new NalUnitContext() {
			@Override
			public H264Context getH264Context() {
				return ctx;
			}
		};
	}
}
