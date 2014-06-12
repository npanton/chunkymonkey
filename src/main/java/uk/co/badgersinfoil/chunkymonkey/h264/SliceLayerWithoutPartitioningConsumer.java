package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.h264.SliceLayerWithoutPartitioningNalUnitConsumer.SliceLayerWithoutPartitioningContext;

public interface SliceLayerWithoutPartitioningConsumer {

	static final SliceLayerWithoutPartitioningConsumer NULL = new SliceLayerWithoutPartitioningConsumer() {
		@Override
		public void header(MediaContext consumerContext, SliceHeader header) { }
		@Override
		public MediaContext createContext(SliceLayerWithoutPartitioningContext ctx) { return null; }
	};

	void header(MediaContext consumerContext, SliceHeader header);

	MediaContext createContext(SliceLayerWithoutPartitioningContext ctx);
}