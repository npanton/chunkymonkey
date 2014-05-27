package uk.co.badgersinfoil.chunkymonkey.rtp;

/**
 * Utility functions for performing modulo arithmetic on RTP sequence numbers.
 */
public final class RtpUtil {
	private RtpUtil() { }

	/**
	 * Returns the next 16-bit RTP sequence number after the given
	 * sequence number.
	 *
	 * If the value given is 65535 (0xffff), then the retult will be 0.
	 */
	public static int seqInc(int seq) {
		return seqAdd(seq, 1);
	}

	public static int seqAdd(int seq, int delta) {
		if (seq < 0x0 || seq > 0xffff) {
			throw new IllegalArgumentException("sequence number outside alowed range: "+seq);
		}
		return (seq + delta) & 0xffff;
	}

	public static boolean seqWrapLikely(int sequenceNumber, int lastSequenceNumber) {
		return lastSequenceNumber > 0x10000*3/4 && sequenceNumber < 0x10000/4;
	}

	public static int seqDiff(int earlierSequenceNumber, int laterSequenceNumber) {
		if (seqWrapLikely(laterSequenceNumber, earlierSequenceNumber)) {
			laterSequenceNumber += 0x10000;
		} else if (seqWrapLikely(earlierSequenceNumber, laterSequenceNumber)) {
			earlierSequenceNumber += 0x10000;
		}
		return laterSequenceNumber - earlierSequenceNumber;
	}

	public static boolean seqLikelyEarlier(int likelyEarlier, int likelyLater) {
		if (seqWrapLikely(likelyLater, likelyEarlier)) {
			likelyLater += 0x10000;
		} else if (seqWrapLikely(likelyEarlier, likelyLater)) {
			likelyEarlier += 0x10000;
		}

		return likelyEarlier < likelyLater;
	}

	public static boolean seqLikelyLater(int likelyEarlier, int likelyLater) {
		if (seqWrapLikely(likelyLater, likelyEarlier)) {
			likelyLater += 0x10000;
		} else if (seqWrapLikely(likelyEarlier, likelyLater)) {
			likelyEarlier += 0x10000;
		}

		return likelyEarlier > likelyLater;
	}
}
