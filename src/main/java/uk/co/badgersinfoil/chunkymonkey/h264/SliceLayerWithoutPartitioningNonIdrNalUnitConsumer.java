package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * NB incomplete - only processes and makes available the slice header, not
 * the data in the body of the slice.
 */
public class SliceLayerWithoutPartitioningNonIdrNalUnitConsumer implements NalUnitConsumer {

	public static class SliceLayerWithoutPartitioningNonIdrContext implements NalUnitContext {

		private H264Context parent;
		public NALUnit currentUnit;
		private ByteBuf buf = Unpooled.buffer();
		H264BitBuf bits;
		public MediaContext consumerContext;

		public SliceLayerWithoutPartitioningNonIdrContext(H264Context parent) {
			this.parent = parent;
		}

		@Override
		public H264Context getH264Context() {
			return parent;
		}

		@Override
		public Locator getLocator() {
			return parent.getLocator();
		}
	}

	private SliceLayerWithoutPartitioningNonIdrConsumer consumer;
	private Reporter rep = Reporter.NULL;

	public SliceLayerWithoutPartitioningNonIdrNalUnitConsumer(SliceLayerWithoutPartitioningNonIdrConsumer consumer) {
		this.consumer = consumer;
	}

	public void setReporter(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void start(NalUnitContext nctx, NALUnit u) {
		if (u.nalRefIdc() == 0) {
			rep.carp(nctx.getLocator(), "nal_ref_idc should not be 0 when nal_unit_type is %s: got %d", u.nalUnitType(), u.nalRefIdc());
		}
		SliceLayerWithoutPartitioningNonIdrContext ctx = (SliceLayerWithoutPartitioningNonIdrContext)nctx;
		ctx.currentUnit = u;
		ctx.bits = new H264BitBuf(ctx.buf);
	}

	@Override
	public void data(NalUnitContext nctx, ByteBuf buf, int offset, int length) {
		SliceLayerWithoutPartitioningNonIdrContext ctx = (SliceLayerWithoutPartitioningNonIdrContext)nctx;
		if (ctx.bits == null) {
			return;
		}
		ctx.buf.writeBytes(buf, offset, length);
		// TODO: horrible hack - don't want to buffer and parse the
		//       whole slice, just to look at the header (at the
		//       moment), so once we've got 40 bytes (which should be
		//       enough for the header, I hope) 'ctx.bits=null' will
		//       stop this method collecting any more of the slice data
		if (ctx.buf.readableBytes() > 40) {
			SliceHeader header = SliceHeader.parse(ctx, ctx.bits, rep);
			consumer.header(ctx.consumerContext, header);
			ctx.bits = null;
		}
	}

	@Override
	public void end(NalUnitContext nctx) {
		SliceLayerWithoutPartitioningNonIdrContext ctx = (SliceLayerWithoutPartitioningNonIdrContext)nctx;
		ctx.buf.clear();
	}

	@Override
	public void continuityError(NalUnitContext nctx) {
		SliceLayerWithoutPartitioningNonIdrContext ctx = (SliceLayerWithoutPartitioningNonIdrContext)nctx;
		ctx.buf.clear();
		ctx.bits = null;
	}

	@Override
	public NalUnitContext createContext(H264Context hctx) {
		SliceLayerWithoutPartitioningNonIdrContext ctx = new SliceLayerWithoutPartitioningNonIdrContext(hctx);
		ctx.consumerContext = consumer.createContext(ctx);
		return ctx;
	}

}
