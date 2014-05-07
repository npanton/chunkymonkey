package uk.co.badgersinfoil.chunkymonkey.ts;

import static org.junit.Assert.*;
import org.junit.Test;


public class StreamTypeTest {
	@Test
	public void basic() {
		assertEquals(StreamType.ADTS.getType(), 0x0F);
		assertSame(StreamType.ADTS, StreamType.forIndex(0x0F));
		assertNotNull(StreamType.forIndex(0x00));
	}
}
