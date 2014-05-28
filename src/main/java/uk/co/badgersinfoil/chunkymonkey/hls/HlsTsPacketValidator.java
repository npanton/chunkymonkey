package uk.co.badgersinfoil.chunkymonkey.hls;

import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.ts.TSContext;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacketConsumer;

public class HlsTsPacketValidator implements TSPacketConsumer {

	public static class HlsTsPacketValidatorContext implements TSContext {

		private TSContext parent;
		public int packetCount = 0;
		public int patCount = 0;

		public HlsTsPacketValidatorContext(TSContext parent) {
			this.parent = parent;
		}
	}

	private Reporter rep;

	public HlsTsPacketValidator(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void packet(TSContext ctx, TSPacket packet) {
		HlsTsPacketValidatorContext vctx = (HlsTsPacketValidatorContext)ctx;
		if (vctx.packetCount == 0 && packet.PID() != 0) {
			rep.carp(packet.getLocator(), "First packet should be PAT (i.e. PID 0), but has PID %d instead", packet.PID());
		}
		if (packet.PID() == 0) {
			vctx.patCount++;
		}
		vctx.packetCount++;
	}

	@Override
	public void end(TSContext context) {
		HlsTsPacketValidatorContext vctx = (HlsTsPacketValidatorContext)context;
		if (vctx.patCount == 0) {
			System.err.println("No PAT seen in HLS segment");
		}
	}

	@Override
	public TSContext createContext(TSContext parent) {
		return new HlsTsPacketValidatorContext(parent);
	}
}
