package uk.co.badgersinfoil.chunkymonkey.adts;

import io.netty.buffer.ByteBuf;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.ts.ElementryContext;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket.DataAlignment;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket.Parsed;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket.PtsDtsFlags;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket.Timestamp;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket;
import static uk.co.badgersinfoil.chunkymonkey.TestUtil.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AdtsPesConsumerTest {

	private static final String SYNC = "fff";
	@Mock
	MediaContext parentCtx;
	@Mock
	ADTSContext ctx;
	@Mock
	private AdtsFrameConsumer consumer;

	private AdtsPesConsumer adts;

	@Before
	public void setup() {
		when(consumer.createContext(any(MediaContext.class))).thenReturn(ctx);
		adts = new AdtsPesConsumer(consumer);
	}

	@Test
	public void context() {
		// when
		ElementryContext ctx = adts.createContext(parentCtx);

		// then
		verify(consumer).createContext(ctx);
	}

	@Test
	public void frameLengthTooShort() {
		// given a frame length of 0 (buried in those header bytes),
		PESPacket pesPacket = mockPacket(SYNC+"000000000000000", DataAlignment.ALIGNED);
		ElementryContext ctx = adts.createContext(parentCtx);

		// when
		adts.start(ctx, pesPacket);

		// then
		verify(consumer, never()).frame(any(ADTSContext.class), any(ADTSFrame.class));
	}

	@Test
	public void payloadLength() {
		// given a frame length of 10 (3 byte payload),
		PESPacket pesPacket = mockPacket(SYNC+"15C80015FFC 010203", DataAlignment.ALIGNED);
		ElementryContext ctx = adts.createContext(parentCtx);

		// when
		adts.start(ctx, pesPacket);

		// then,
		ArgumentCaptor<ADTSFrame> cap = ArgumentCaptor.forClass(ADTSFrame.class);
		verify(consumer).frame(any(ADTSContext.class), cap.capture());
		assertEquals(3, cap.getValue().payload().readableBytes());
	}

	@Test
	public void nonAlignedPesPacket() {
		// given a frame length of 10 with partial payload in this packet,
		PESPacket pesPacket1 = mockPacket(SYNC+"15C80015FFC 0102", DataAlignment.ALIGNED);
		// and given the following packet contains the rest of the frame,
		PESPacket pesPacket2 = mockPacket("03", DataAlignment.NOT_ALIGNED);
		ElementryContext ctx = adts.createContext(parentCtx);

		// when the two packets are processed,
		adts.start(ctx, pesPacket1);
		adts.start(ctx, pesPacket2);

		// then we get a complete ADTS frame as a result,
		ArgumentCaptor<ADTSFrame> cap = ArgumentCaptor.forClass(ADTSFrame.class);
		verify(consumer).frame(any(ADTSContext.class), cap.capture());
		// TODO: unsafe in general to assume that a single frame() call
		//       will result (some of the data could come via
		//       continuation() for instance)
		assertEquals(3, cap.getValue().payload().readableBytes());
	}

	@Test
	public void continuation() {
		// given a frame length of 10 with partial payload in this packet,
		PESPacket pesPacket1 = mockPacket(SYNC+"15C80015FFC 0102", DataAlignment.ALIGNED);
		// and given a continuation of the PES packet in another TS packet,
		TSPacket tsPk = mock(TSPacket.class);
		ByteBuf buf = hexToBuf("03");
		ElementryContext ctx = adts.createContext(parentCtx);

		// when the two packets are processed,
		adts.start(ctx, pesPacket1);
		adts.continuation(ctx, tsPk, buf);

		// then we get a complete ADTS frame as a result,
		ArgumentCaptor<ADTSFrame> cap = ArgumentCaptor.forClass(ADTSFrame.class);
		verify(consumer).frame(any(ADTSContext.class), cap.capture());
		// TODO: unsafe in general to assume that a single frame() call
		//       will result (some of the data could come via
		//       continuation() for instance)
		assertEquals(3, cap.getValue().payload().readableBytes());
	}

	private PESPacket mockPacket(String hex, DataAlignment alignment) {
		PESPacket pk = mock(PESPacket.class);
		when(pk.isParsed()).thenReturn(true);
		Parsed parsed = mock(Parsed.class);
		when(pk.getParsedPESPaload()).thenReturn(parsed);
		when(parsed.getContent()).thenReturn(hexToBuf(hex));
		when(parsed.dataAlignmentIndicator()).thenReturn(alignment);
		when(parsed.ptsDdsFlags()).thenReturn(PtsDtsFlags.PTS_ONLY);
		when(parsed.pts()).thenReturn(new Timestamp(0, 0, 0, 0, 0, 0));
		return pk;
	}
}
