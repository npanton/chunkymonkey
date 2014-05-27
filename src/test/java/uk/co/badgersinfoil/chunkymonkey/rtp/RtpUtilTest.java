package uk.co.badgersinfoil.chunkymonkey.rtp;

import org.junit.Test;
import static org.junit.Assert.*;
import static uk.co.badgersinfoil.chunkymonkey.rtp.RtpUtil.*;

public class RtpUtilTest {

	@Test
	public void testSeqInc() {
		assertEquals(1, seqInc(0));
		assertEquals(0, seqInc(0xffff));
	}

	@Test
	public void testSeqAdd() {
		assertEquals(2, seqAdd(0, 2));
		assertEquals(1, seqAdd(0xfffe, 3));
	}

	@Test
	public void testSeqWrapLikely() {
		assertTrue(seqWrapLikely(3, 0xfffe));
		assertFalse(seqWrapLikely(0xfffe, 3));
	}

	@Test
	public void testSeqDiff() {
		assertEquals(1, seqDiff(0, 1));
		assertEquals(1, seqDiff(0xffff, 0));
	}

	@Test
	public void testSeqLikelyEarlier() {
		assertTrue(seqLikelyEarlier(1, 2));
		assertTrue(seqLikelyEarlier(65422, 3));
		assertFalse(seqLikelyEarlier(3, 65422));
	}
}
