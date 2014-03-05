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
		NALUnit expectedUnit = new NALUnit(null, hexToBuf("16" + "010203"));
		verify(defaultConsumer).unit(eq(ctx), eq(expectedUnit));
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
		NALUnit expectedUnit = new NALUnit(null, hexToBuf("16" + "010203040506"));
		verify(defaultConsumer).unit(eq(ctx), eq(expectedUnit));
	}

	/**
	 * Attempt to find unhandled edge cases by sliding the split between
	 * to adjacent buffers across all possible offsets.  e.g. if the
	 * delimiter between NAL units is split between two subsequent buffers,
	 * will the parser fail?
	 */
	@Test
	public void continuationVsDelimiter() throws Exception {
		H264Context ctx = (H264Context)h264PesConsumer.createContext();
		ByteBuf content = hexToBuf(
				 "000001"    // start code
				+"15"        // forbidden_zero_bit, nal_ref_idc, nal_unit_type
				+"010203"    // data
				+"000001"    // delimiter
				+"16"        // forbidden_zero_bit, nal_ref_idc, nal_unit_type
				+"040506"    // more data
			);
		for (int i=3 ; i<content.readableBytes(); i++) {
			PESPacket pesPacket = mockPacket(content.slice(0, i));
			h264PesConsumer.start(ctx, pesPacket);
			ByteBuf payload = content.slice(i, content.readableBytes() - i);
			TSPacket packet = mock(TSPacket.class);
			h264PesConsumer.continuation(ctx, packet, payload);
			h264PesConsumer.end(ctx);
			NALUnit expectedUnitOne = new NALUnit(null, hexToBuf("15" + "010203"));
			verify(defaultConsumer, atLeastOnce()).unit(eq(ctx), eq(expectedUnitOne));
			NALUnit expectedUnitTwo = new NALUnit(null, hexToBuf("16" + "040506"));
			verify(defaultConsumer, atLeastOnce()).unit(eq(ctx), eq(expectedUnitTwo));
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
