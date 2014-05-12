package uk.co.badgersinfoil.chunkymonkey.h264;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.h264.NALUnit.UnitType;
import uk.co.badgersinfoil.chunkymonkey.h264.NalUnitConsumer.NalUnitContext;
import uk.co.badgersinfoil.chunkymonkey.ts.ElementryContext;
import uk.co.badgersinfoil.chunkymonkey.ts.PESConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket.Parsed;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket;

/**
 * Support AVC data within an elementary stream
 */
public class H264PesConsumer implements PESConsumer {

	private static final ByteBuf FAKE_ZERO_BYTES = Unpooled.wrappedBuffer(new byte[] {0x00, 0x00});
	private Map<UnitType,NalUnitConsumer> nalUnitConsumers = new HashMap<>();
	private NalUnitConsumer defaultNalUnitConsumer = NalUnitConsumer.NULL;

	public static class PesNalUnitLocator implements Locator {

		private Locator parent;
		private int index;

		public PesNalUnitLocator(Locator parent, int index) {
			this.parent = parent;
			this.index = index;
		}

		@Override
		public Locator getParent() {
			return parent;
		}

		@Override
		public String toString() {
			return "NAL Unit #"+index+"\n  at "+parent.toString();
		}
	}

	public H264PesConsumer(Map<UnitType,NalUnitConsumer> nalUnitConsumers) {
		this.nalUnitConsumers.putAll(nalUnitConsumers);
	}

	public void setDefaultNalUnitConsumer(NalUnitConsumer defaultNalUnitConsumer) {
		this.defaultNalUnitConsumer = defaultNalUnitConsumer;
	}

	private NalUnitConsumer getNalUnitConsumerFor(UnitType type) {
		NalUnitConsumer consumer = nalUnitConsumers.get(type);
		if (consumer == null) {
			return defaultNalUnitConsumer;
		}
		return consumer;
	}

	@Override
	public void start(ElementryContext ctx, PESPacket pesPacket) {
		H264Context hCtx = (H264Context)ctx;
		if ((pesPacket.streamId() & 0b1111_0000) != 0b1110_0000) {
			System.err.println("Unexpected stream_id for H264 video "+Integer.toBinaryString(pesPacket.streamId()));
		}
		hCtx.setPesPacket(pesPacket);
		Parsed parsed = pesPacket.getParsedPESPaload();
		ByteBuf add = parsed.getContent();
		hCtx.start();
		dataArrived(hCtx, add);
	}

	@Override
	public void continuation(ElementryContext ctx, TSPacket packet, ByteBuf payload) {
		H264Context hCtx = (H264Context)ctx;
		if (hCtx.isIgnoreRest()) {
//System.err.println("  cont ignored");
			return;
		}
		dataArrived(hCtx, payload);
	}

	public static enum ParseState {
		START,
		START_ONE_ZERO,
		START_TWO_ZERO,
		START_THREE_ZERO,
		UNIT_HEADER,
		IN_UNIT,
		IN_UNIT_ONE_ZERO,
		IN_UNIT_TWO_ZERO,
		IN_UNIT_THREE_ZERO
	}

	/**
	 * H264 'NAL Unit' push parser implementation.  Ongoing parse
	 * state is externalised into the given H264Context object, so that
	 * data for the parser to handle can be delivered over multiple
	 * successive calls to this method, as it becomes available from the
	 * lower levels of the protocol stack.
	 */
	private void dataArrived(H264Context ctx, ByteBuf data) {
		int zeroSeqStart = -1;
		int dataStartOffset;
		if (ctx.state() == ParseState.IN_UNIT) {
			// we were in the middle of emitting data when the last
			// buffer ended, so we will continue emitting from the
			// start of this buffer,
			dataStartOffset = 0;
		} else {
			// 'dataStartOffset will need to be initialised
			// properly when we parse the next NAL unit header,
			dataStartOffset = -1;
		}
		final int max = data.readableBytes();
		outer: for (int i=0; i<max; i++) {
			int b = data.getUnsignedByte(i);
			switch (ctx.state()) {
			case START:
				switch (b) {
				case 0x00:
					ctx.state(ParseState.START_ONE_ZERO);
					break;
				default:
					throw new RuntimeException("TODO: handle invalid start code properly");
				}
				break;
			case START_ONE_ZERO:
				switch (b) {
				case 0x00:
					ctx.state(ParseState.START_TWO_ZERO);
					break;
				default:
					throw new RuntimeException("TODO: handle invalid start code properly");
				}
				break;
			case START_TWO_ZERO:
				switch (b) {
				case 0x00:
					ctx.state(ParseState.START_THREE_ZERO);
					break;
				case 0x01:
					ctx.state(ParseState.UNIT_HEADER);
					break;
				default:
					System.err.println("bad start code 0x"+Integer.toHexString(b));
					ctx.setIgnoreRest(true);
				}
				break;
			case START_THREE_ZERO:
				if (b == 0x01) {
					ctx.state(ParseState.UNIT_HEADER);
				} else {
					System.err.println("bad start code 0x"+Integer.toHexString(b));
					ctx.setIgnoreRest(true);
				}
				break;
			case UNIT_HEADER:
				header(ctx, b);
				ctx.state(ParseState.IN_UNIT);
				dataStartOffset = i + 1;
				break;
			case IN_UNIT:
				if (b == 0x00) {
					ctx.state(ParseState.IN_UNIT_ONE_ZERO);
					zeroSeqStart = i;
				} else {
					for (i++; i<max; i++) {
						b = data.getUnsignedByte(i);
						if (b == 0) {
							i--;
							continue outer;
						}
					}
				}
				break;
			case IN_UNIT_ONE_ZERO:
				if (b == 0x00) {
					ctx.state(ParseState.IN_UNIT_TWO_ZERO);
				} else {
					if (i == 0) {
						// the first 0x00 was in the
						// previous buf, which has now
						// gone.  send a zero through
						// to consumer
						data(ctx, FAKE_ZERO_BYTES, 0, 1);
						dataStartOffset = 0;
					}
					ctx.state(ParseState.IN_UNIT);
					zeroSeqStart = -1;
				}
				break;
			case IN_UNIT_TWO_ZERO:
				switch (b) {
				case 0x00:
					ctx.state(ParseState.IN_UNIT_THREE_ZERO);
					break;
				case 0x01:
					if (dataStartOffset != -1 && (zeroSeqStart-dataStartOffset) != 0) {
						data(ctx, data, dataStartOffset, zeroSeqStart);
					} // else the data was presumably
					  // at the end of the previous buffer
					endPrev(ctx);
					dataStartOffset = i + 1;
					ctx.state(ParseState.UNIT_HEADER);
					zeroSeqStart = -1;
					break;
				case 0x03:
					// 'emulation prevention sequence'.
					// deliver preceding zero-bytes, but
					// not the 0x03 byte itself
					if (i < 2) {
						// the first 0x00 was in the
						// previous buf, which has now
						// gone.  send a zero through
						// to consumer
						data(ctx, FAKE_ZERO_BYTES, 0, 2-i);
						dataStartOffset = 0;
					}
					if (dataStartOffset != -1) {
						data(ctx, data, dataStartOffset, i);
					} // else the data was presumably
					  // at the end of the previous buffer
					dataStartOffset = i + 1;
					ctx.state(ParseState.IN_UNIT);
					zeroSeqStart = -1;
					break;
				default:
					if (i < 2) {
						// the first 0x00 was in the
						// previous buf, which has now
						// gone.  send a zero through
						// to consumer
						data(ctx, FAKE_ZERO_BYTES, 0, 2-i);
						dataStartOffset = 0;
					}
					ctx.state(ParseState.IN_UNIT);
					zeroSeqStart = -1;
					break;
				}
				break;
			case IN_UNIT_THREE_ZERO:
				switch (b) {
				case 0x00:
					// stay in this state
					break;
				case 0x01:
					if (dataStartOffset != -1) {
						data(ctx, data, dataStartOffset, zeroSeqStart);
					}
					endPrev(ctx);
					ctx.state(ParseState.UNIT_HEADER);
					zeroSeqStart = -1;
					break;
				default:
					System.err.println("bad byte value following three or more zero bytes: 0x"+Integer.toHexString(b)+" offset "+i+"\n"+ByteBufUtil.hexDump(data));
					ctx.setIgnoreRest(true);
					break outer;
				}
				break;
			}
		}
		if (!ctx.isIgnoreRest() && dataStartOffset != -1) {
			int end = zeroSeqStart == -1 ? data.readableBytes() : zeroSeqStart;
			if (end - dataStartOffset != 0) {
				data(ctx, data, dataStartOffset, end);
			}
		}
	}

	private void header(H264Context ctx, int b) {
		ctx.continuityError(false);
		PesNalUnitLocator loc = new PesNalUnitLocator(ctx.getPesPacket().getLocator(), ctx.nextUnitIndex());
		NALUnit u = new NALUnit(loc, b);
		NalUnitConsumer consumer = getNalUnitConsumerFor(u.nalUnitType());
		NalUnitContext nalCtx = ctx.nalContext(u.nalUnitType());
		ctx.setNalUnit(u);
		ctx.setCurrentNalUnitConsumer(consumer);
		ctx.setCurrentNalUnitContext(nalCtx);
		consumer.start(nalCtx, u);
	}

	private void data(H264Context ctx, ByteBuf data, int dataStartOffset, int zeroSeqStart) {
//		if (dataStartOffset == -1) {
//			throw new IllegalStateException("Parser bug: dataStartOffset not yet initialized");
//		}
//		if (zeroSeqStart == -1) {
//			throw new IllegalStateException("Parser bug: zeroSeqStart not yet initialized");
//		}
		if (ctx.continuityError()) {
			// ignore further data for this NAL unit (but we may
			// be able to continue parsing subsequent NAL Units
			// later in the PES packet)
			return;
		}
		NalUnitConsumer consumer = ctx.getCurrentNalUnitConsumer();
		NalUnitContext nalCtx = ctx.getCurrentNalContext();
		int len = zeroSeqStart - dataStartOffset;
		consumer.data(nalCtx, data, dataStartOffset, len);
	}

	private void endPrev(H264Context ctx) {
		NalUnitConsumer consumer = ctx.getCurrentNalUnitConsumer();
		NalUnitContext nalCtx = ctx.getCurrentNalContext();
		if (!ctx.continuityError() && inUnit(ctx.state())) {
			consumer.end(nalCtx);
		}
	}

	private boolean inUnit(ParseState state) {
		return state == ParseState.IN_UNIT
		    || state == ParseState.IN_UNIT_ONE_ZERO
		    || state == ParseState.IN_UNIT_TWO_ZERO
		    || state == ParseState.IN_UNIT_THREE_ZERO;
	}

	@Override
	public void end(ElementryContext ctx) {
		H264Context hCtx = (H264Context)ctx;
		if (hCtx.isIgnoreRest()) {
System.err.println("H264PesConsumer.end() - end of H264 PES packet we decided to ignore");
			return;
		}

		endPrev(hCtx);
	}

	@Override
	public void continuityError(ElementryContext ctx) {
		H264Context hCtx = (H264Context)ctx;
		NalUnitConsumer consumer = hCtx.getCurrentNalUnitConsumer();
		if (consumer != null) {
			consumer.continuityError(hCtx.getCurrentNalContext());
		}
		hCtx.continuityError(true);
	}

	@Override
	public ElementryContext createContext() {
		H264Context ctx = new H264Context();
		for (Entry<UnitType, NalUnitConsumer> e : nalUnitConsumers.entrySet()) {
			ctx.addNalContext(e.getKey(), e.getValue());
		}
		ctx.setDefaultNalContext(defaultNalUnitConsumer.createContext(ctx));
		return ctx;
	}
}
