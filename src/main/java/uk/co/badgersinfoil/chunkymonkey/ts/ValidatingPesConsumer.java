package uk.co.badgersinfoil.chunkymonkey.ts;

import io.netty.buffer.ByteBuf;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket.Parsed;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket.Timestamp;

public class ValidatingPesConsumer implements PESConsumer {

	public static class ValidatingElementryContext implements
			ElementryContext {
		private Timestamp lastDts;
		public Integer lastStreamId;
	}

	private Reporter rep;

	public ValidatingPesConsumer(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void start(ElementryContext ctx, PESPacket pesPacket) {
		ValidatingElementryContext vCtx = (ValidatingElementryContext)ctx;
		if (pesPacket.packetStartCodePrefix() != 1) {
			rep.carp(pesPacket.getLocator(), "start_code_prefix should be 0x1, got: ", pesPacket.packetStartCodePrefix());
		}
		if (vCtx.lastStreamId != null && vCtx.lastStreamId != pesPacket.streamId()) {
			rep.carp(pesPacket.getLocator(), "stream_id changed: %d, was previously %d", pesPacket.streamId(), vCtx.lastStreamId);
		}
		vCtx.lastStreamId = pesPacket.streamId();
		if (pesPacket.isParsed()) {
			Parsed payload = pesPacket.getParsedPESPaload();
			if (payload.ptsDdsFlags().isDtsPresent()) {
				if (vCtx.lastDts != null && vCtx.lastDts.isValid() && payload.dts().isValid()) {
					long diff = payload.dts().getTs() - vCtx.lastDts.getTs();
					if (diff < 0) {
						rep.carp(pesPacket.getLocator(), "DTS went backwards (wraparound?): %s -> %s", vCtx.lastDts, payload.dts());
					} else if (diff == 0) {
						rep.carp(pesPacket.getLocator(), "DTS failed to advance: %s", payload.dts());
					}
				}
				vCtx.lastDts = payload.dts();
			}
		}
	}

	@Override
	public void continuation(ElementryContext ctx, TSPacket packet, ByteBuf payload) {
		// TODO Auto-generated method stub

	}
	@Override
	public void end(ElementryContext ctx) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void continuityError(ElementryContext ctx) {
		// TODO: in order to use Reporter instance, we need a Locator
		//       object to describe where the discontinuity occurred,
		//       but our API does not yet define this
	}

	@Override
	public ElementryContext createContext() {
		return new ValidatingElementryContext();
	}
}
