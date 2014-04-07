package uk.co.badgersinfoil.chunkymonkey.ts;

public interface RTPErrorHandler {
	
	public static final RTPErrorHandler NULL = new RTPErrorHandler() {
		@Override
		public void unexpectedSsrc(long expectedSsrc, long actualSsrc) { }
		@Override
		public void unexpectedSequenceNumber(int expectedSeq, int actualSeq) { }
		@Override
		public void timeWentBackwards(long lastTimestamp, long timestamp) { }
		@Override
		public void timestampJumped(long lastTimestamp, long timestamp) { }
	};

	void unexpectedSsrc(long expectedSsrc, long actualSsrc);

	void unexpectedSequenceNumber(int expectedSeq, int actualSeq);

	void timeWentBackwards(long lastTimestamp, long timestamp);

	void timestampJumped(long lastTimestamp, long timestamp);

}
