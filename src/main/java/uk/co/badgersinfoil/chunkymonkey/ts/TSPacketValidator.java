package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.HashMap;
import java.util.Map;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Alert;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket.ProgramClockReference;

public class TSPacketValidator implements TSPacketConsumer {

	@LogFormat("PCR base went backwards {tickDiff}ticks: {thisPcr} (was {lastPcr})")
	public static class PcrWentBackwardsAlert extends Alert { }
	@LogFormat("PCR base did not increase: {pcr}")
	public static class PcrStuckAlert extends Alert { }

	public class TSPacketValidatorContext implements MediaContext {
		private MediaContext parentContext;
		private Map<Integer,ProgramClockReference> lastPCRs = new HashMap<>();

		public TSPacketValidatorContext(MediaContext parentContext) {
			this.parentContext = parentContext;
		}

		@Override
		public Locator getLocator() {
			return parentContext.getLocator();
		}
	}

	private static final int ADAPTATION_FIELD_MAX_LENGTH = 183;
	private static final int ADAPTATION_FIELD_WITH_CONTENT_MAX_LENGTH = 182;
	private Reporter rep;

	public TSPacketValidator(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void packet(MediaContext ctx, TSPacket packet) {
		TSPacketValidatorContext vctx = (TSPacketValidatorContext)ctx;
		if (packet.transportErrorIndicator()) {
			rep.carp(vctx.getLocator(), "Transport error indicator flag present");
		}
		checkPCR(vctx, packet);
		checkAdaptationFieldLength(vctx, packet);
	}

	private static final int PCR_MAX_INTERVAL_TICKS = 9000; // 100ms @90kHz

	private void checkPCR(TSPacketValidatorContext vctx, TSPacket packet) {
		if (packet.adaptionControl().adaptionFieldPresent() && packet.getAdaptationField().pcrFlag()) {
			ProgramClockReference pcr = packet.getAdaptationField().pcr();
			if (vctx.lastPCRs.containsKey(packet.PID())) {
				ProgramClockReference lastPCR = vctx.lastPCRs.get(packet.PID());
				long diff = pcr.getPcrBase() - lastPCR.getPcrBase();
				if (diff < 0 && !isWrapLikely(diff)) {
					new PcrWentBackwardsAlert()
						.with("tickDiff", diff)
						.with("thisPcr", pcr)
						.with("lastPcr", lastPCR)
						.at(vctx)
						.to(rep);
				} else if (diff == 0) {
					new PcrStuckAlert()
						.with("pcr", pcr)
						.at(vctx)
						.to(rep);
				} else if (diff > PCR_MAX_INTERVAL_TICKS) {
//					rep.carp(packet.getLocator(), "PCR interval greater than 100ms maximum: %dticks (%s to %s)", diff, pcr.toSexidecimalString(), lastPCR.toSexidecimalString());
				}
			}
			vctx.lastPCRs.put(packet.PID(), pcr);
		}
	}

	private boolean isWrapLikely(long pcrBaseDiff) {
		// TODO: better to build wrap-logic into MediaTimestamp?
		final long ONE_SECOND = 90_000;  // DTS @ 90kHz
		final int DTS_BITS = 33;
		return pcrBaseDiff < 0 && (ONE_SECOND - 1L<<DTS_BITS) > pcrBaseDiff;
	}

	private void checkAdaptationFieldLength(TSPacketValidatorContext vctx, TSPacket packet) {
		if (packet.adaptionControl().adaptionFieldPresent()) {
			int adaptationLen = packet.getAdaptationField().length();
			if (adaptationLen > ADAPTATION_FIELD_MAX_LENGTH) {
				rep.carp(vctx.getLocator(), "Adaptation field too long: %d bytes", adaptationLen);
			} else if (packet.adaptionControl().contentPresent() && adaptationLen > ADAPTATION_FIELD_WITH_CONTENT_MAX_LENGTH) {
//				rep.carp(vctx.getLocator(), "Adaptation field too long given content also preset: %d bytes", adaptationLen);
			}
		}
	}

	@Override
	public void end(MediaContext context) {
		// TODO Auto-generated method stub
	}

	@Override
	public MediaContext createContext(MediaContext parent) {
		return new TSPacketValidatorContext(parent);
	}
}
