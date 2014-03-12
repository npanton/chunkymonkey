package uk.co.badgersinfoil.chunkymonkey.h264;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.Reporter;

@RunWith(MockitoJUnitRunner.class)
public class ValidatingNalUnitConsumerTest {
	@Mock
	private Reporter rep;
	@InjectMocks
	private ValidatingNalUnitConsumer validatingNalUnitConsumer;
	@Mock
	private Locator locator;

	@Test
	public void testGoodForbiddenZeroBit() throws Exception {
		int header = 0b0_00_01000;
		startWithHeader(header);
		verify(rep, never()).carp(eq(locator), any(String.class));
	}

	@Test
	public void testBadForbiddenZeroBit() throws Exception {
		int header = 0b1_00_01000;
		startWithHeader(header);
		verify(rep).carp(eq(locator), any(String.class));
	}

	private void startWithHeader(int header) {
		NALUnit u = new NALUnit(locator, header);
		H264Context ctx = null;
		validatingNalUnitConsumer.start(ctx, u);
	}
}
