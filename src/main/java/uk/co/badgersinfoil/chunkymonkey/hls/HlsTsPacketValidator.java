package uk.co.badgersinfoil.chunkymonkey.hls;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacketConsumer;

public class HlsTsPacketValidator implements TSPacketConsumer {

	public static class HlsTsPacketValidatorContext implements MediaContext {

		private MediaContext parent;
		public int packetCount = 0;
		public int patCount = 0;

		public HlsTsPacketValidatorContext(MediaContext parent) {
			this.parent = parent;
		}

		@Override
		public Locator getLocator() {
			return parent.getLocator();
		}
	}

	private Reporter rep;

	public HlsTsPacketValidator(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void packet(MediaContext ctx, TSPacket packet) {
		HlsTsPacketValidatorContext vctx = (HlsTsPacketValidatorContext)ctx;
		if (vctx.packetCount == 0 && packet.PID() != 0) {
			rep.carp(ctx.getLocator(), "First packet should be PAT (i.e. PID 0), but has PID %d instead", packet.PID());
		}
		if (packet.PID() == 0) {
			vctx.patCount++;
		}
		vctx.packetCount++;
	}

	@Override
	public void end(MediaContext context) {
		HlsTsPacketValidatorContext vctx = (HlsTsPacketValidatorContext)context;
		if (vctx.patCount == 0) {
			System.err.println("No PAT seen in HLS segment");
		}
	}

	@Override
	public MediaContext createContext(MediaContext parent) {
		return new HlsTsPacketValidatorContext(parent);
	}
}
