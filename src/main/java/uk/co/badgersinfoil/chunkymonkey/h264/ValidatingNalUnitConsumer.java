package uk.co.badgersinfoil.chunkymonkey.h264;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.h264.NALUnit.UnitType;

public class ValidatingNalUnitConsumer implements NalUnitConsumer {

	private Reporter rep;

	public ValidatingNalUnitConsumer(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void unit(H264Context ctx, NALUnit u) {
		if (u.nalUnitType() == UnitType.SEI) {
			ByteBuf buf = u.getContent();
//			int payloadType = read
			rep.carp(u.getLocator(), "SEI 0x%s", ByteBufUtil.hexDump(buf));
		}
//		else rep.carp(u.getLocator(), "NALUnit "+u.nalUnitType());
	}
}
