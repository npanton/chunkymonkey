package uk.co.badgersinfoil.chunkymonkey.h264;

public interface SeiHeaderConsumer {

	public static final SeiHeaderConsumer NULL = new SeiHeaderConsumer() {
		@Override
		public void header(H264Context ctx, SeiHeader seiHeader) { }
	};

	void header(H264Context ctx, SeiHeader seiHeader);
}
