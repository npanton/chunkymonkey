package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.h264.SliceLayerWithoutPartitioningNalUnitConsumer.SliceLayerWithoutPartitioningContext;

public interface SliceLayerWithoutPartitioningConsumer {

	void header(MediaContext consumerContext, SliceHeader header);

	MediaContext createContext(SliceLayerWithoutPartitioningContext ctx);
}