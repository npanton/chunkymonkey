package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import io.netty.buffer.ByteBuf;

public interface NalUnitConsumer {

	public interface NalUnitContext extends MediaContext {
		H264Context getH264Context();
	}
	NalUnitConsumer NULL = new NalUnitConsumer() {
		@Override
		public void start(NalUnitContext ctx, NALUnit u) {  }
		@Override
		public void data(NalUnitContext ctx, ByteBuf buf, int offset, int length) {  }
		@Override
		public void end(NalUnitContext ctx) {  }
		@Override
		public void continuityError(NalUnitContext ctx) { }
		@Override
		public NalUnitContext createContext(final H264Context ctx) {
			return new NalUnitContext() {
				@Override
				public H264Context getH264Context() {
					// TODO Auto-generated method stub
					return ctx;
				}
				@Override
				public Locator getLocator() {
					return ctx.getLocator();
				}
			};
		}
	};

	void start(NalUnitContext ctx, NALUnit u);
	void data(NalUnitContext ctx, ByteBuf buf, int offset, int length);
	void end(NalUnitContext ctx);
	void continuityError(NalUnitContext ctx);
	NalUnitContext createContext(H264Context ctx);
}
