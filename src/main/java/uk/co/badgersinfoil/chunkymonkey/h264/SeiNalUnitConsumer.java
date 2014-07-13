package uk.co.badgersinfoil.chunkymonkey.h264;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.h264.SeiHeaderConsumer.SeiHeaderContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class SeiNalUnitConsumer implements NalUnitConsumer {

	public class SeiLocator implements Locator {

		private Locator parent;
		private int headerNo;

		public SeiLocator(Locator parent, int headerNo) {
			this.parent = parent;
			this.headerNo = headerNo;
		}

		@Override
		public String toString() {
			return "SEI Header #"+headerNo+"\n  at "+parent.toString();
		}

		@Override
		public Locator getParent() {
			return parent;
		}
	}

	public class SeiNalUnitContext implements NalUnitContext {

		private H264Context ctx;

		public SeiNalUnitContext(H264Context ctx) {
			this.ctx = ctx;
		}
		@Override
		public H264Context getH264Context() {
			return ctx;
		}
		private ByteBuf seiBuffer = Unpooled.buffer();
		private Map<Integer, SeiHeaderContext> contexts = new HashMap<>();
		private SeiHeaderContext defaultSeiContext;
		private int headerNo = 0;

		public ByteBuf seiBuffer() {
			return seiBuffer;
		}
		public void addSeiContext(Integer type, SeiHeaderConsumer consumer) {
			contexts.put(type, consumer.createContext(this));
		}
		public void setDefaultSeiContext(SeiHeaderContext context) {
			this.defaultSeiContext = context;
		}
		public SeiHeaderContext getContextForType(int type) {
			SeiHeaderContext context = contexts.get(type);
			return context == null ? defaultSeiContext : context;
		}

		@Override
		public Locator getLocator() {
			return new SeiLocator(ctx.getLocator(), headerNo);
		}
	}

	private Map<Integer, SeiHeaderConsumer> seiConsumers = new HashMap<>();
	private SeiHeaderConsumer defaultSeiConsumer = SeiHeaderConsumer.NULL;
	private Reporter rep = Reporter.NULL;

	public SeiNalUnitConsumer(Map<Integer, SeiHeaderConsumer> seiConsumers) {
		this.seiConsumers.putAll(seiConsumers);
	}

	public void setReporter(Reporter rep) {
		this.rep = rep;
	}

	private SeiHeaderConsumer getSeiConsumerForType(int type) {
		SeiHeaderConsumer consumer = seiConsumers.get(type);
		if (consumer == null) {
			return defaultSeiConsumer;
		}
		return consumer;
	}

	@Override
	public void start(NalUnitContext ctx, NALUnit u) {
		if (u.nalRefIdc() != 0) {
			rep.carp(ctx.getLocator(), "nal_ref_idc should be 0 when nal_unit_type is %s: got %d", u.nalUnitType(), u.nalRefIdc());
		}
	}
	@Override
	public void data(NalUnitContext nalCtx, ByteBuf buf, int offset, int length) {
		SeiNalUnitContext ctx = (SeiNalUnitContext)nalCtx;
		// we could attempt to push-parse the SEI header list, but
		// I assume SEI headers are not a significant amount of data,
		// and take the lazy approach of just buffering the entire
		// SEI NAL unit and then parse the headers at the end.
		ctx.seiBuffer().writeBytes(buf, offset, length);
	}
	@Override
	public void end(NalUnitContext nalCtx) {
		SeiNalUnitContext ctx = (SeiNalUnitContext)nalCtx;
		ByteBuf buf = ctx.seiBuffer();
		int left;
		while ((left = buf.readableBytes()) > 0) {
			// TODO: not sure how best to identify that we need to
			// parse 'rbsp_trailing_bits', other than by assuming
			// they will be in the last byte, as implemented here,
			if (left == 1) {
				rbspTrailingBits(buf);
				break;
			}
			int type = readVar(buf);
			int size = readVar(buf);
			ctx.headerNo++;
			SeiHeaderContext headerCtx = ctx.getContextForType(type);
			getSeiConsumerForType(type).header(headerCtx, new SeiHeader(type, buf.slice(buf.readerIndex(), size)));
			buf.skipBytes(size);
		}
		buf.clear();
	}

	@Override
	public void continuityError(NalUnitContext nalCtx) {
		SeiNalUnitContext ctx = (SeiNalUnitContext)nalCtx;
		ByteBuf buf = ctx.seiBuffer();
		buf.clear();
	}

	private void rbspTrailingBits(ByteBuf buf) {
		short trailing = buf.readUnsignedByte();
		if (trailing != 0b10000000) {
			// TODO: proper reporting
			System.err.println("bad rbsp_trailing_bits: 0x"+Integer.toHexString(trailing));
		}
	}

	private int readVar(ByteBuf buf) {
		int val = 0;
		int b;
		do {
			b = buf.readUnsignedByte();
			val += b;
		} while (b == 0xff);

		return val;
	}

	@Override
	public NalUnitContext createContext(H264Context ctx) {
		SeiNalUnitContext seiCtx = new SeiNalUnitContext(ctx);
		for (Entry<Integer, SeiHeaderConsumer> e : seiConsumers.entrySet()) {
			seiCtx.addSeiContext(e.getKey(), e.getValue());
		}
		seiCtx.setDefaultSeiContext(defaultSeiConsumer.createContext(seiCtx));
		return seiCtx;
	}
}
