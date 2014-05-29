package uk.co.badgersinfoil.chunkymonkey.h264;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.h264.NALUnit.UnitType;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket.Parsed;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket;

@RunWith(MockitoJUnitRunner.class)
public class H264PesConsumerTest {

	/**
	 * Mocks NalUnitConsumer in order to collect together the whole NAL
	 * Unit body independent of the number and size of buffers the body
	 * was delivered in.
	 */
	private static class MockNalUnitConsumer implements NalUnitConsumer {
		public static class UnitHolder {
			public UnitHolder(NALUnit u) {
				unit = u;
			}
			public NALUnit unit;
			public ByteBuf data = Unpooled.buffer();
			private boolean ended;

			public void assertCompleteUnitWas(NALUnit expected, ByteBuf expectedData) {
				Assert.assertEquals(expected, unit);
				Assert.assertTrue("end() was not called", ended);
				if (!expectedData.equals(data)) {
					Assert.fail("expected "+ByteBufUtil.hexDump(expectedData)+", but got "+ByteBufUtil.hexDump(data));
				}
			}
		}
		private boolean started;
		private List<UnitHolder> units = new ArrayList<>();

		@Override
		public void start(NalUnitContext ctx, NALUnit u) {
			started = true;
			units.add(new UnitHolder(u));
		}

		@Override
		public void end(NalUnitContext ctx) {
			Assert.assertTrue("start() not called before end()", started);
			Assert.assertTrue("end() called with no payload delivered", lastUnit().data.readableBytes() > 0);
			lastUnit().ended = true;
			started = false;
		}

		@Override
		public void data(NalUnitContext ctx, ByteBuf buf, int offset, int length) {
			Assert.assertTrue("length should be > 0", length > 0);
			Assert.assertTrue("start() not called before data()", started);
			lastUnit().data.writeBytes(buf, offset, length);
		}

		private UnitHolder lastUnit() {
			return unit(units.size()-1);
		}
		public UnitHolder unit(int i) {
			return units.get(i);
		}

		@Override
		public void continuityError(NalUnitContext ctx) {
		}

		@Override
		public NalUnitContext createContext(H264Context ctx) {
			return null;
		}
	}
	private MockNalUnitConsumer defaultConsumer = new MockNalUnitConsumer();

	@Mock
	private MediaContext parentContext;

	private H264PesConsumer h264PesConsumer;

	private static final Map<UnitType, NalUnitConsumer> nalUnitConsumers
		= Collections.emptyMap();;

	@Before
	public void setup() {
		h264PesConsumer = new H264PesConsumer(nalUnitConsumers);
		h264PesConsumer.setDefaultNalUnitConsumer(defaultConsumer);
	}

	@Test
	public void basic() throws Exception {
		H264Context ctx = (H264Context)h264PesConsumer.createContext(null);
		ByteBuf content = hexToBuf(
				 "000001"    // start code
				+"16"        // forbidden_zero_bit, nal_ref_idc, nal_unit_type
				+"010203"    // data
			);
		PESPacket pesPacket = mockPacket(content);
		h264PesConsumer.start(ctx, pesPacket);
		h264PesConsumer.end(ctx);
		NALUnit expectedUnit = new NALUnit(0x16);
		defaultConsumer.unit(0).assertCompleteUnitWas(expectedUnit, hexToBuf("010203"));
	}

	@Test
	public void emulationPrevention() throws Exception {
		H264Context ctx = (H264Context)h264PesConsumer.createContext(parentContext);
		ByteBuf content = hexToBuf(
				 "000001"    // start code
				+"16"        // forbidden_zero_bit, nal_ref_idc, nal_unit_type
				+"01020000030003"    // data
			);
		PESPacket pesPacket = mockPacket(content);
		h264PesConsumer.start(ctx, pesPacket);
		h264PesConsumer.end(ctx);
		NALUnit expectedUnit = new NALUnit(0x16);
		defaultConsumer.unit(0).assertCompleteUnitWas(expectedUnit, hexToBuf("010200000003"));
	}

	@Test
	public void continuation() throws Exception {
		H264Context ctx = (H264Context)h264PesConsumer.createContext(parentContext);
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
		NALUnit expectedUnit = new NALUnit(0x16);
		defaultConsumer.unit(0).assertCompleteUnitWas(expectedUnit, hexToBuf("010203040506"));
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
		H264Context ctx = (H264Context)h264PesConsumer.createContext(parentContext);
		ByteBuf content = hexToBuf(
				 "000001"    // start code
				+"15"        // forbidden_zero_bit, nal_ref_idc, nal_unit_type
				+"010203"    // data
				+"000001"    // delimiter
				+"16"        // forbidden_zero_bit, nal_ref_idc, nal_unit_type
				+"040506"    // more data
			);
		TSPacket packet = mock(TSPacket.class);
		for (int i=1 ; i<content.readableBytes(); i++) {
			defaultConsumer = new MockNalUnitConsumer();
			h264PesConsumer = new H264PesConsumer(nalUnitConsumers);
			h264PesConsumer.setDefaultNalUnitConsumer(defaultConsumer);

			PESPacket pesPacket = mockPacket(content.slice(0, i));
			ByteBuf payload = content.slice(i, content.readableBytes() - i);
			// when,
			h264PesConsumer.start(ctx, pesPacket);
			h264PesConsumer.continuation(ctx, packet, payload);
			h264PesConsumer.end(ctx);
			// then,
			NALUnit expectedUnitOne = new NALUnit(0x15);
			defaultConsumer.unit(0).assertCompleteUnitWas(expectedUnitOne, hexToBuf("010203"));
			NALUnit expectedUnitTwo = new NALUnit(0x16);
			defaultConsumer.unit(1).assertCompleteUnitWas(expectedUnitTwo, hexToBuf("040506"));
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
