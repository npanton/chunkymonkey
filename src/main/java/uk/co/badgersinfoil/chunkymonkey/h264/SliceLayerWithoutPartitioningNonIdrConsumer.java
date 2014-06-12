package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.h264.SliceLayerWithoutPartitioningNonIdrNalUnitConsumer.SliceLayerWithoutPartitioningNonIdrContext;

public interface SliceLayerWithoutPartitioningNonIdrConsumer {

	static final SliceLayerWithoutPartitioningNonIdrConsumer NULL = new SliceLayerWithoutPartitioningNonIdrConsumer() {
		@Override
		public void header(MediaContext consumerContext, SliceHeader header) { }
		@Override
		public MediaContext createContext(SliceLayerWithoutPartitioningNonIdrContext ctx) { return null; }
	};

	void header(MediaContext consumerContext, SliceHeader header);

	MediaContext createContext(SliceLayerWithoutPartitioningNonIdrContext ctx);
}