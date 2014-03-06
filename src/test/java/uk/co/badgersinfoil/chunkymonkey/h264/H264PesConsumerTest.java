package uk.co.badgersinfoil.chunkymonkey.h264;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import uk.co.badgersinfoil.chunkymonkey.h264.NALUnit.UnitType;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket.Parsed;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket;

@RunWith(MockitoJUnitRunner.class)
public class H264PesConsumerTest {
	@Mock
	private NalUnitConsumer defaultConsumer;

	private H264PesConsumer h264PesConsumer;

	@Before
	public void setup() {
		Map<UnitType, NalUnitConsumer> nalUnitConsumers
			= Collections.emptyMap();
		h264PesConsumer = new H264PesConsumer(nalUnitConsumers);
		h264PesConsumer.setDefaultNalUnitConsumer(defaultConsumer);
	}

	@Test
	public void basic() throws Exception {
		H264Context ctx = (H264Context)h264PesConsumer.createContext();
		ByteBuf content = hexToBuf(
				 "000001"    // start code
				+"16"        // forbidden_zero_bit, nal_ref_idc, nal_unit_type
				+"010203"    // data
			);
		PESPacket pesPacket = mockPacket(content);
		h264PesConsumer.start(ctx, pesPacket);
		h264PesConsumer.end(ctx);
		NALUnit expectedUnit = new NALUnit(null, 0x16);
		verify(defaultConsumer).start(eq(ctx), eq(expectedUnit));
		verify(defaultConsumer).data(eq(ctx), eq(hexToBuf("010203")));
		verify(defaultConsumer).end(eq(ctx));
	}

	@Test
	public void continuation() throws Exception {
		H264Context ctx = (H264Context)h264PesConsumer.createContext();
		ByteBuf content = hexToBuf(
				 "000001"    // start code
				+"16"        // forbidden_zero_bit, nal_ref_idc, nal_unit_type
				+"010203"    // data
			);
		PESPacket pesPacket = mockPacket(content);
		h264PesConsumer.start(ctx, pesPacket);
		ByteBuf payload = hexToBuf("040506");
		TSPacket packet = mock(TSPacket.class);
		h264PesConsumer.continuation(ctx, packet, payload);
		h264PesConsumer.end(ctx);
		NALUnit expectedUnit = new NALUnit(null, 0x16);
		verify(defaultConsumer).start(eq(ctx), eq(expectedUnit));
		verify(defaultConsumer).data(eq(ctx), eq(hexToBuf("010203040506")));
		verify(defaultConsumer).end(eq(ctx));
	}

	/**
	 * Attempt to find unhandled edge cases by sliding the split between
	 * to adjacent buffers across all possible offsets.  e.g. if the
	 * delimiter between NAL units is split between two subsequent buffers,
	 * will the parser fail?
	 */
	@Test
	public void continuationVsDelimiter() throws Exception {
		// given,
		H264Context ctx = (H264Context)h264PesConsumer.createContext();
		ByteBuf content = hexToBuf(
				 "000001"    // start code
				+"15"        // forbidden_zero_bit, nal_ref_idc, nal_unit_type
				+"010203"    // data
				+"000001"    // delimiter
				+"16"        // forbidden_zero_bit, nal_ref_idc, nal_unit_type
				+"040506"    // more data
			);
		TSPacket packet = mock(TSPacket.class);
		for (int i=7 ; i<10; i++) {
			PESPacket pesPacket = mockPacket(content.slice(0, i));
			ByteBuf payload = content.slice(i, content.readableBytes() - i);
			// when,
			h264PesConsumer.start(ctx, pesPacket);
			h264PesConsumer.continuation(ctx, packet, payload);
			h264PesConsumer.end(ctx);
			// then,
			NALUnit expectedUnitOne = new NALUnit(null, 0x15);
			verify(defaultConsumer, atLeastOnce()).start(eq(ctx), eq(expectedUnitOne));
			verify(defaultConsumer, atLeastOnce()).data(eq(ctx), eq(hexToBuf("010203")));
			verify(defaultConsumer, atLeastOnce()).end(eq(ctx));
			NALUnit expectedUnitTwo = new NALUnit(null, 0x16);
			verify(defaultConsumer, atLeastOnce()).start(eq(ctx), eq(expectedUnitTwo));
			verify(defaultConsumer, atLeastOnce()).data(eq(ctx), eq(hexToBuf("040506")));
			verify(defaultConsumer, atLeastOnce()).end(eq(ctx));
		}
	}

	/**
	 * Creates a mock H264 PES packet with the given content in its parsed
	 * payload.
	 */
	private PESPacket mockPacket(ByteBuf content) {
		PESPacket pesPacket = mock(PESPacket.class);
		when(pesPacket.streamId()).thenReturn(0b1110_0001);
		Parsed payload = mock(PESPacket.Parsed.class);
		when(pesPacket.getParsedPESPaload()).thenReturn(payload);
		when(payload.getContent()).thenReturn(content);
		return pesPacket;
	}

	private ByteBuf hexToBuf(String hex) throws DecoderException {
		return Unpooled.wrappedBuffer(Hex.decodeHex(hex.toCharArray()));
	}
}
