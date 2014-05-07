package uk.co.badgersinfoil.chunkymonkey.ts;

import uk.co.badgersinfoil.chunkymonkey.ts.PIDFilterPacketConsumer.FilterEntry;
import uk.co.badgersinfoil.chunkymonkey.ts.ProgramAssociationTable.ProgramEntry;

public class PATConsumer implements TSPacketConsumer {

	public class PATContext implements TSContext, TransportContextProvider {

		private TSContext parent;

		public PATContext(TSContext parent) {
			this.parent = parent;
		}

		@Override
		public TransportContext getTransportContext() {
			return ((TransportContextProvider)parent).getTransportContext();
		}

	}

	private static final int PID_PAT = 0;

	private PIDFilterPacketConsumer filter;
	private PmtTSPacketConsumerConsumer pmtConsumer;
	private TSPacketConsumer networkConsumer;

	public PATConsumer(PIDFilterPacketConsumer filter, PmtTSPacketConsumerConsumer pmtConsumer) {
		this.filter = filter;
		this.pmtConsumer = pmtConsumer;
		networkConsumer = TSPacketConsumer.NULL;
	}

	@Override
	public void packet(TSContext ctx, TSPacket packet) {
		TransportContext tctx = ((TransportContextProvider)ctx).getTransportContext();
		if (packet.PID() != PID_PAT) {
			throw new RuntimeException("PID expexcted to be 0, got: "+packet.PID());
		}
		// TODO: create in constructor?
		ProgramAssociationTable pat = new ProgramAssociationTable(packet.getPayload());
		ProgramEntry entry = pat.entries();
		while (entry.next()) {
			handleEntry(tctx, entry);
		}
	}

	private void handleEntry(TransportContext tctx, ProgramEntry entry) {
		switch (entry.kind()) {
		case PROGRAM_MAP:
			handleProgramEntry(tctx, entry);
			break;
		case NETWORK:
			handleNetworkEntry(tctx, entry);
			break;
		}
	}

	private void handleProgramEntry(TransportContext tctx,
	                                ProgramEntry entry)
	{
		FilterEntry current = filter.getCurrent(tctx, entry.programMapPid());
		if (current==null || current.getConsumer().equals(TSPacketConsumer.NULL) || isDifferentProgram(entry, current)) {
			if (current != null) {
				current.getConsumer().end(current.getContext());
			}
			ProgramTSContext programCtx = pmtConsumer.createContext(tctx, entry);
			filter.filter(tctx, entry.programMapPid(), new FilterEntry(pmtConsumer, programCtx));
		}
	}

	private void handleNetworkEntry(TransportContext tctx,
	                                ProgramEntry entry)
	{
		FilterEntry current = filter.getCurrent(tctx, entry.networkPid());
		if (current==null || !current.getConsumer().equals(TSPacketConsumer.NULL)) {
			System.out.println("PAT: network pid entries not yet handled ("+entry.networkPid()+")");
			TSContext networkCtx = networkConsumer.createContext(tctx);
			filter.filter(tctx, entry.networkPid(), new FilterEntry(networkConsumer, networkCtx));
		}
	}

	private boolean isDifferentProgram(ProgramEntry entry, FilterEntry current) {
		return ((ProgramTSContext)current.getContext()).getPid() != entry.programMapPid();
	}

	@Override
	public void end(TSContext ctx) {
		PATContext c = (PATContext)ctx;

	}

	@Override
	public TSContext createContext(TSContext parent) {
		return new PATContext(parent);
	}
}
