package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * NB incomplete - only processes and makes available the slice header, not
 * the data in the body of the slice.
 */
public class SliceLayerWithoutPartitioningNalUnitConsumer implements NalUnitConsumer {

	public static class SliceLayerWithoutPartitioningContext implements NalUnitContext {

		private H264Context parent;
		public NALUnit currentUnit;
		private ByteBuf buf = Unpooled.buffer();
		H264BitBuf bits;
		public MediaContext consumerContext;

		public SliceLayerWithoutPartitioningContext(H264Context parent) {
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

	private SliceLayerWithoutPartitioningConsumer consumer;

	public SliceLayerWithoutPartitioningNalUnitConsumer(SliceLayerWithoutPartitioningConsumer consumer) {
		this.consumer = consumer;
	}

	@Override
	public void start(NalUnitContext nctx, NALUnit u) {
		SliceLayerWithoutPartitioningContext ctx = (SliceLayerWithoutPartitioningContext)nctx;
		ctx.currentUnit = u;
		ctx.bits = new H264BitBuf(ctx.buf);
	}

	@Override
	public void data(NalUnitContext nctx, ByteBuf buf, int offset, int length) {
		SliceLayerWithoutPartitioningContext ctx = (SliceLayerWithoutPartitioningContext)nctx;
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
			SliceHeader header = new SliceHeader(ctx.bits);
			consumer.header(ctx.consumerContext, header);
			ctx.bits = null;
		}
	}

	@Override
	public void end(NalUnitContext nctx) {
		SliceLayerWithoutPartitioningContext ctx = (SliceLayerWithoutPartitioningContext)nctx;
		ctx.buf.clear();
	}

	@Override
	public void continuityError(NalUnitContext nctx) {
		SliceLayerWithoutPartitioningContext ctx = (SliceLayerWithoutPartitioningContext)nctx;
		ctx.buf.clear();
		ctx.bits = null;
	}

	@Override
	public NalUnitContext createContext(H264Context hctx) {
		SliceLayerWithoutPartitioningContext ctx = new SliceLayerWithoutPartitioningContext(hctx);
		ctx.consumerContext = consumer.createContext(ctx);
		return ctx;
	}

}
