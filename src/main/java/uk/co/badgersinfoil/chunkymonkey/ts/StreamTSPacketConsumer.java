package uk.co.badgersinfoil.chunkymonkey.ts;

import uk.co.badgersinfoil.chunkymonkey.ts.ProgramMapTable.StreamDescriptorIterator;

public interface StreamTSPacketConsumer extends TSPacketConsumer {

	public StreamTSContext createContext(ProgramTSContext ctx, StreamDescriptorIterator streamDesc);
}
