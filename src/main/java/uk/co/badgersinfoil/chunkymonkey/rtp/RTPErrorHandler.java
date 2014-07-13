package uk.co.badgersinfoil.chunkymonkey.rtp;

import uk.co.badgersinfoil.chunkymonkey.event.Locator;

public interface RTPErrorHandler {

	public static final RTPErrorHandler NULL = new RTPErrorHandler() {
		@Override
		public void unexpectedSsrc(Locator loc, long expectedSsrc, long actualSsrc) { }
		@Override
		public void unexpectedSequenceNumber(Locator loc, int expectedSeq, int actualSeq) { }
		@Override
		public void timeWentBackwards(Locator loc, long lastTimestamp, long timestamp) { }
		@Override
		public void timestampJumped(Locator loc, long lastTimestamp, long timestamp) { }
	};

	void unexpectedSsrc(Locator loc, long expectedSsrc, long actualSsrc);

	void unexpectedSequenceNumber(Locator loc, int expectedSeq, int actualSeq);

	void timeWentBackwards(Locator loc, long lastTimestamp, long timestamp);

	void timestampJumped(Locator loc, long lastTimestamp, long timestamp);

}
