package uk.co.badgersinfoil.chunkymonkey.ts;

import uk.co.badgersinfoil.chunkymonkey.ts.ProgramMapTable.StreamDescriptorIterator;

public interface StreamProcBuilder {

	TSPacketConsumer create(StreamDescriptorIterator i);
}
