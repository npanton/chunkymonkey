package uk.co.badgersinfoil.chunkymonkey.rtp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.rtp.RtpParser.RtpContext;
import uk.co.badgersinfoil.chunkymonkey.ts.BufferTransportStreamParser;

@RunWith(MockitoJUnitRunner.class)
public class RtpBufferTest {

	@Mock
	private RtpContext ctx;
	@Mock
	private BufferTransportStreamParser consumer;

	@Test
	public void add() throws Exception {
		RtpBuffer b = new RtpBuffer(4);
		RtpPacket p = mockPacket(1);
		b.add(ctx, consumer, p);
		assertEquals(0, b.diffSeq());
		assertEquals(1, b.size());
		verify(consumer).buffer(any(MediaContext.class), any(ByteBuf.class), any(Locator.class));
	}

	private RtpPacket mockPacket(int seq) {
		RtpPacket p = mock(RtpPacket.class);
		when(p.sequenceNumber()).thenReturn(seq);
		when(p.payload()).thenReturn(Unpooled.EMPTY_BUFFER);
		return p;
	}

	@Test
	public void hugeGap() throws Exception {
		RtpBuffer b = new RtpBuffer(3);
		b.add(ctx, consumer, mockPacket(1));
		RtpPacket p = mockPacket(5);
		b.add(ctx, consumer, p);
		assertEquals(3, b.minSeqNumber());
		assertEquals(3, b.size());
		assertEquals(2, b.diffSeq());
		assertNull(b.getSeq(1));
		assertNull(b.getSeq(2));
		assertNull(b.getSeq(3));
		assertNull(b.getSeq(4));
		assertEquals(Unpooled.EMPTY_BUFFER, b.getSeq(5));
	}
}
