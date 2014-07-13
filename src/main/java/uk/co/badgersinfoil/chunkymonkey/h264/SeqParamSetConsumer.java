package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.h264.SeqParamSetNalUnitConsumer.SeqParamSetNalUnitContext;

public interface SeqParamSetConsumer {

	SeqParamSetConsumer NULL = new SeqParamSetConsumer() {

		@Override
		public MediaContext createContext(SeqParamSetNalUnitContext parent) {
			return null;
		}

		@Override
		public void seqParamSet(MediaContext consumerContext,
		                        SeqParamSet params)
		{
		}
	};

	MediaContext createContext(SeqParamSetNalUnitContext parent);

	void seqParamSet(MediaContext consumerContext, SeqParamSet params);
}
