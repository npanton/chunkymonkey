package uk.co.badgersinfoil.chunkymonkey.h264;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.h264.NalUnitConsumer.NalUnitContext;

@RunWith(MockitoJUnitRunner.class)
public class ValidatingNalUnitConsumerTest {
	@Mock
	private Reporter rep;
	@Mock
	private H264Context parentContext;
	@InjectMocks
	private ValidatingNalUnitConsumer validatingNalUnitConsumer;

	@Test
	public void testGoodForbiddenZeroBit() throws Exception {
		int header = 0b0_00_01000;
		startWithHeader(header);
		verify(rep, never()).carp(any(Locator.class), any(String.class));
	}

	@Test
	public void testBadForbiddenZeroBit() throws Exception {
		int header = 0b1_00_01000;
		startWithHeader(header);
		verify(rep).carp(any(Locator.class), any(String.class));
	}

	private void startWithHeader(int header) {
		NALUnit u = new NALUnit(header);
		NalUnitContext ctx = validatingNalUnitConsumer.createContext(parentContext);
		validatingNalUnitConsumer.start(ctx, u);
	}
}
