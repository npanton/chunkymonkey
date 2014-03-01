package uk.co.badgersinfoil.chunkymonkey.h264;

public interface NalUnitConsumer {

	NalUnitConsumer NULL = new NalUnitConsumer() {
		@Override
		public void unit(H264Context ctx, NALUnit u) { }
	};

	void unit(H264Context ctx, NALUnit u);
}
