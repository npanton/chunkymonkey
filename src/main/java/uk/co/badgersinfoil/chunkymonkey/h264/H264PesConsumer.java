package uk.co.badgersinfoil.chunkymonkey.h264;

import java.util.HashMap;
import java.util.Map;
import io.netty.buffer.ByteBuf;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.h264.NALUnit.UnitType;
import uk.co.badgersinfoil.chunkymonkey.ts.ElementryContext;
import uk.co.badgersinfoil.chunkymonkey.ts.PESConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket.Parsed;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket;

/**
 * Support AVC data within an elementary stream
 */
public class H264PesConsumer implements PESConsumer {

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
System.err.println("  cont ignored");
			return;
		}
		dataArrived(hCtx, payload);
	}

	private void dataArrived(H264Context hCtx, ByteBuf data) {
		hCtx.getBuf().writeBytes(data);
		while (true) {
			ByteBuf buf = hCtx.getBuf();
			if (!hCtx.nalStarted()) {
				int startCode = buf.readUnsignedMedium();
				if (startCode == 0) {
					startCode = buf.readUnsignedByte();
				}
				if (startCode != 1) {
System.err.println("bad start code 0x"+Integer.toHexString(startCode));
					hCtx.setIgnoreRest(true);
				}
				hCtx.nalStarted(true);
				hCtx.getBuf().markReaderIndex();
			}
			boolean found = scanForNalEnd(hCtx);
			if (!found) {
				break;
			}
			int end = buf.readerIndex();
			buf.resetReaderIndex();
			int len = end - buf.readerIndex();
//System.err.println("end found, len="+len);
			PesNalUnitLocator loc = new PesNalUnitLocator(hCtx.getPesPacket().getLocator(), hCtx.nextUnitIndex());
			NALUnit u = new NALUnit(loc, hCtx.getBuf().slice(hCtx.getBuf().readerIndex(), len));
			handle(hCtx, u);
			buf.skipBytes(len);
			hCtx.nalStarted(false);
		}
	}

	/**
	 * Attempts to find the next occurrence of either the 3-byte sequence
	 * 0x000001 or the 4-byte sequence 0x00000001 in the context's buffer.
	 * 
	 * Returns true, and leaves the readerIndex at the location of the
	 * sequence when found, or returns false and leaves the buffer's
	 * readerIndex at the location scanned to so far otherwise.  This
	 * method does not alter the buffer's mark (so the caller may use this
	 * to track where in the buffer the NAL unit started).  When false is
	 * returned, the caller is expected to invoke this method again once it
	 * has received more data and added it to the context's buffer.
	 */
	private boolean scanForNalEnd(H264Context hCtx) {
		int code = 0xffffffff;
		ByteBuf buf = hCtx.getBuf();
		int count = 0;
		while (buf.isReadable()) {
			code <<= 8;
			code |= buf.readUnsignedByte();
			count++;
			// test the bottom 3 bytes of 'code', masking-out the
			// high byte,
			if ((code & 0xffffff) == 0x000001) {
				// if 'code' is still '1' when considering the
				// high byte too, then this is a 4-byte code,
				// otherwise the high byte must contain data,
				// and this is a 3-byte code,
				int codeLength = code == 1 ? 4 : 3;
				buf.readerIndex(buf.readerIndex() - codeLength);
				return true;
			}
		}
		// rewind, so that we can try the last 2 bytes again next time
		// TODO: alternatively, store 'code' state into hCtx
		buf.readerIndex(buf.readerIndex() - Math.min(count, 2));
		return false;
	}

	@Override
	public void end(ElementryContext ctx) {
		H264Context hCtx = (H264Context)ctx;
		if (hCtx.isIgnoreRest()) {
System.err.println("  end: ignored");
			return;
		}
		if (hCtx.getBuf().readableBytes() == 0) {
System.err.println("  end: no more data");
			return;
		}
		PesNalUnitLocator loc = new PesNalUnitLocator(hCtx.getPesPacket().getLocator(), hCtx.nextUnitIndex());
		ByteBuf buf = hCtx.getBuf();
		int end = buf.readerIndex() + buf.readableBytes();
		buf.resetReaderIndex();
		int len = end - buf.readerIndex();
//System.err.println("end implied, len="+len);
		NALUnit u = new NALUnit(loc, hCtx.getBuf().slice(hCtx.getBuf().readerIndex(), len));
		handle(hCtx, u);
		buf.clear();
	}

	private void handle(H264Context ctx, NALUnit u) {
		getNalUnitConsumerFor(u.nalUnitType()).unit(ctx, u);
	}

	@Override
	public ElementryContext createContext() {
		return new H264Context();
	}
}
