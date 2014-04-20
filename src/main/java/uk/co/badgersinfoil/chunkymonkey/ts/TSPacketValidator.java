package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.HashMap;
import java.util.Map;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket.ProgramClockReference;

public class TSPacketValidator implements TSPacketConsumer {
	
	public class ValidatorTSContext implements TSContext {
		private Map<Integer,ProgramClockReference> lastPCRs = new HashMap<>();
	}

	private static final int ADAPTATION_FIELD_MAX_LENGTH = 183;
	private static final int ADAPTATION_FIELD_WITH_CONTENT_MAX_LENGTH = 182;
	private Reporter rep;
	
	public TSPacketValidator(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void packet(TSContext ctx, TSPacket packet) {
		ValidatorTSContext vctx = (ValidatorTSContext)ctx;
		if (packet.transportErrorIndicator()) {
			rep.carp(packet.getLocator(), "Transport error indicator flag present");
		}
		checkPCR(vctx, packet);
		checkAdaptationFieldLength(packet);
	}
	
	private static final int PCR_MAX_INTERVAL_NANOS = 100000;

	private void checkPCR(ValidatorTSContext vctx, TSPacket packet) {
		// HLS requirement,
//		if (packet.getLocator().getPacketNo() == 0 && packet.PID() != 0) {
//			rep.carp(packet.getLocator(), "First packet should be PMT (i.e. PID 0), but has PID %d instead", packet.PID());
//		}
		if (packet.adaptionControl().adaptionFieldPresent() && packet.getAdaptationField().pcrFlag()) {
			ProgramClockReference pcr = packet.getAdaptationField().pcr();
			if (vctx.lastPCRs.containsKey(packet.PID())) {
				ProgramClockReference lastPCR = vctx.lastPCRs.get(packet.PID());
				long diff = pcr.toNanoseconds() - lastPCR.toNanoseconds();
				if (diff < 0) {
					rep.carp(packet.getLocator(), "PCR went backwards %dms (wraparound?): %s (was %s)", diff/1000, pcr, lastPCR);
				} else if (diff == 0) {
					rep.carp(packet.getLocator(), "PCR did not increase: %s", pcr);
				} else if (diff > PCR_MAX_INTERVAL_NANOS) {
//					rep.carp(packet.getLocator(), "PCR interval greater than 100ms maximum: %dms (%s to %s)", diff/1000, pcr.toSexidecimalString(), lastPCR.toSexidecimalString());
				}
			}
			vctx.lastPCRs.put(packet.PID(), pcr);
		}
	}

	private void checkAdaptationFieldLength(TSPacket packet) {
		if (packet.adaptionControl().adaptionFieldPresent()) {
			int adaptationLen = packet.getAdaptationField().length();
			if (adaptationLen > ADAPTATION_FIELD_MAX_LENGTH) {
				rep.carp(packet.getLocator(), "Adaptation field too long: %d bytes", adaptationLen);
			} else if (packet.adaptionControl().contentPresent() && adaptationLen > ADAPTATION_FIELD_WITH_CONTENT_MAX_LENGTH) {
//				rep.carp(packet.getLocator(), "Adaptation field too long given content also preset: %d bytes", adaptationLen);
			}
		}
	}

	@Override
	public void end(TSContext context) {
		// TODO Auto-generated method stub
	}

	@Override
	public TSContext createContext(TSContext parent) {
		return new ValidatorTSContext();
	}
}
