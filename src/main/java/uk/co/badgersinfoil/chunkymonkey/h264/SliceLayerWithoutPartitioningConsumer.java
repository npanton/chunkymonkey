package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.h264.SliceLayerWithoutPartitioningNalUnitConsumer.SliceLayerWithoutPartitioningContext;
import uk.co.badgersinfoil.chunkymonkey.ts.TSContext;

public interface SliceLayerWithoutPartitioningConsumer {

	void header(TSContext consumerContext, SliceHeader header);

	TSContext createContext(SliceLayerWithoutPartitioningContext ctx);
}