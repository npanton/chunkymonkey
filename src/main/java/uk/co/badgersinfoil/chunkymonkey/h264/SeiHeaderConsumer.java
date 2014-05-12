package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.h264.NalUnitConsumer.NalUnitContext;

public interface SeiHeaderConsumer {

	public interface SeiHeaderContext {
		NalUnitContext getNalUnitContext();
	}

	public static final SeiHeaderConsumer NULL = new SeiHeaderConsumer() {
		@Override
		public void header(SeiHeaderContext ctx, SeiHeader seiHeader) { }

		@Override
		public SeiHeaderContext createContext(final NalUnitContext ctx) {
			return new SeiHeaderContext() {
				@Override
				public NalUnitContext getNalUnitContext() {
					return ctx;
				}
			};
		}
	};

	void header(SeiHeaderContext ctx, SeiHeader seiHeader);

	SeiHeaderContext createContext(NalUnitContext ctx);
}
