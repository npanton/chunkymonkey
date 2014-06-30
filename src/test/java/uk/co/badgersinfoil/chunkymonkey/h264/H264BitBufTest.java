package uk.co.badgersinfoil.chunkymonkey.h264;

import org.junit.Test;
import static org.junit.Assert.*;
import static uk.co.badgersinfoil.chunkymonkey.TestUtil.*;

public class H264BitBufTest {

	@Test
	public void moreRbspDataAtByteBoundry() throws Exception {
		H264BitBuf b = new H264BitBuf(hexToBuf("0080"));
		b.readBits(8);
		assertFalse(b.moreRbspData());
	}

	@Test
	public void moreRbspDataAtEnd() throws Exception {
		H264BitBuf b = new H264BitBuf(hexToBuf("00"));
		b.readBits(8);
		assertFalse(b.moreRbspData());
	}
}
