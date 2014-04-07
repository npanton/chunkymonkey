package uk.co.badgersinfoil.chunkymonkey.h264;

import io.netty.buffer.ByteBuf;

public interface NalUnitConsumer {

	NalUnitConsumer NULL = new NalUnitConsumer() {
		@Override
		public void start(H264Context ctx, NALUnit u) {  }
		@Override
		public void data(H264Context ctx, ByteBuf buf, int offset, int length) {  }
		@Override
		public void end(H264Context ctx) {  }
		@Override
		public void continuityError(H264Context hCtx) { }
	};

	void start(H264Context ctx, NALUnit u);
	void data(H264Context ctx, ByteBuf buf, int offset, int length);
	void end(H264Context ctx);
	void continuityError(H264Context ctx);
}
