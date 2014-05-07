package uk.co.badgersinfoil.chunkymonkey.ts;

import uk.co.badgersinfoil.chunkymonkey.ts.PIDFilterPacketConsumer.FilterEntry;
import uk.co.badgersinfoil.chunkymonkey.ts.ProgramAssociationTable.ProgramEntry;
import uk.co.badgersinfoil.chunkymonkey.ts.ProgramAssociationTable.ProgramEntryKind;

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
	private StreamProcRegistry registery;

	public PATConsumer(PIDFilterPacketConsumer filter, StreamProcRegistry registery) {
		this.filter = filter;
		this.registery = registery;
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
			if (entry.kind() == ProgramEntryKind.PROGRAM_MAP) {
				FilterEntry current = filter.getCurrent(tctx, entry.programMapPid());
				if (current==null || current.getConsumer().equals(TSPacketConsumer.NULL) || isDifferentProgram(entry, current)) {
					if (current != null) {
						current.getConsumer().end(current.getContext());
					}
					PMTConsumer pmtConsumer = new PMTConsumer(filter, registery);
					ProgramTSContext programCtx = pmtConsumer.createContext(tctx, entry);
					filter.filter(tctx, entry.programMapPid(), new FilterEntry(pmtConsumer, programCtx));
				}
			} else {
				FilterEntry current = filter.getCurrent(tctx, entry.networkPid());
				if (current==null || !current.getConsumer().equals(TSPacketConsumer.NULL)) {
					System.out.println("PAT: network pid entries not yet handled ("+entry.networkPid()+")");
					TSPacketConsumer consumer = TSPacketConsumer.NULL;
					filter.filter(tctx, entry.networkPid(), new FilterEntry(consumer, consumer.createContext(tctx)));
				}
			}
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
