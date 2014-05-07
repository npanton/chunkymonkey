package uk.co.badgersinfoil.chunkymonkey.ts;

import uk.co.badgersinfoil.chunkymonkey.ts.ProgramAssociationTable.ProgramEntry;
import uk.co.badgersinfoil.chunkymonkey.ts.ProgramMapTable.StreamDescriptorIterator;

public class PmtTSPacketConsumerConsumer implements TSPacketConsumer {

	public class PMTContext implements TSContext {

	}

	private PIDFilterPacketConsumer filter;
	private StreamProcRegistry registry;

	public PmtTSPacketConsumerConsumer(PIDFilterPacketConsumer filter,
	                   StreamProcRegistry registry)
	{
		this.filter = filter;
		this.registry = registry;
	}

	@Override
	public void packet(TSContext ctx, TSPacket packet) {
		ProgramTSContext progCtx = (ProgramTSContext)ctx;
		ProgramMapTable pmt = progCtx.lastPmt();
		if (packet.payloadUnitStartIndicator()) {
			if (pmt != null && !pmt.isComplete()) {
				System.err.println("Last PMT incomplete at start of new PMT");
			}
			pmt = new ProgramMapTable(packet.getLocator(), packet.getPayload());
			progCtx.lastPmt(pmt);
		} else {
			if (pmt == null) {
				// ignore PMT continuation when payload start missed
			} else {
				if (pmt.isComplete()) {
					System.err.println("Last PMT complete, but more payload arrived");
					return;
				}
				pmt.appendPayload(packet.getPayload());
			}
		}
		if (pmt != null && pmt.isComplete()) {
			handlePMT(progCtx, pmt);
		}
	}

	private void handlePMT(ProgramTSContext progCtx, ProgramMapTable pmt) {
		StreamDescriptorIterator i = pmt.streamDescriptors();
		while (i.hasNext()) {
			StreamTSPacketConsumer newConsumer = registry.getStreamHandler(i.streamType());
			PIDFilterPacketConsumer.FilterEntry entry = filter.getCurrent(progCtx.getTransportContext(), i.elementryPID());
			if (entry==null || !newConsumer.equals(entry.getConsumer())) {
				if (entry != null && entry.getConsumer() != TSPacketConsumer.NULL) {
System.err.println("replace "+entry.getConsumer()+" for PID "+i.elementryPID());
					entry.getConsumer().end(entry.getContext());
					progCtx.removeStream(entry);
				}
				StreamTSContext streamCtx = newConsumer.createContext(progCtx, i);
				PIDFilterPacketConsumer.FilterEntry newEntry
					= new PIDFilterPacketConsumer.FilterEntry(newConsumer, streamCtx);
				filter.filter(progCtx.getTransportContext(), i.elementryPID(), newEntry);
				progCtx.addStream(entry);
			}
			i.next();
		}
	}

	public ProgramTSContext createContext(TransportContext ctx, ProgramEntry entry) {
		return new ProgramTSContext(ctx);
	}

	@Override
	public void end(TSContext ctx) {
		ProgramTSContext progCtx = (ProgramTSContext)ctx;
	}

	@Override
	public TSContext createContext(TSContext parent) {
		return new PMTContext();
	}
}
