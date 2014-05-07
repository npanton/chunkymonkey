package uk.co.badgersinfoil.chunkymonkey.ts;

import uk.co.badgersinfoil.chunkymonkey.ts.ProgramMapTable.StreamDescriptorIterator;

public class PmtConsumerImpl implements PmtConsumer {

	private PIDFilterPacketConsumer filter;
	private StreamProcRegistry registry;

	public PmtConsumerImpl(PIDFilterPacketConsumer filter,
	                       StreamProcRegistry registry)
	{
		this.filter = filter;
		this.registry = registry;
	}

	@Override
	public void handle(ProgramTSContext progCtx, ProgramMapTable pmt) {
		StreamDescriptorIterator i = pmt.streamDescriptors();
		while (i.hasNext()) {
			handleStreamDescriptor(progCtx, i);
			i.next();
		}
	}

	private void handleStreamDescriptor(ProgramTSContext progCtx,
	                                    StreamDescriptorIterator desc)
	{
		StreamTSPacketConsumer newConsumer = registry.getStreamHandler(desc.streamType());
		PIDFilterPacketConsumer.FilterEntry entry = filter.getCurrent(progCtx.getTransportContext(), desc.elementryPID());
		if (entry==null || !newConsumer.equals(entry.getConsumer())) {
			if (entry != null && entry.getConsumer() != TSPacketConsumer.NULL) {
System.err.println("replace "+entry.getConsumer()+" for PID "+desc.elementryPID());
				entry.getConsumer().end(entry.getContext());
				progCtx.removeStream(entry);
			}
			StreamTSContext streamCtx = newConsumer.createContext(progCtx, desc);
			PIDFilterPacketConsumer.FilterEntry newEntry
				= new PIDFilterPacketConsumer.FilterEntry(newConsumer, streamCtx);
			filter.filter(progCtx.getTransportContext(), desc.elementryPID(), newEntry);
			progCtx.addStream(entry);
		}
	}
}
