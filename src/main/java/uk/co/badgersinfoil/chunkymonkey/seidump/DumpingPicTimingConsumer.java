package uk.co.badgersinfoil.chunkymonkey.seidump;

import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.h264.PicTimingConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.PicTimingHeader;
import uk.co.badgersinfoil.chunkymonkey.h264.PicTimingSeiConsumer.PicTimingSeiContext;

public class DumpingPicTimingConsumer implements PicTimingConsumer {

	@Override
	public void picTiming(PicTimingContext ctx, PicTimingHeader picTiming) {
		System.out.println(picTiming);
	}

	@Override
	public PicTimingContext createContext(final PicTimingSeiContext ctx) {
		return new PicTimingContext() {
			@Override
			public PicTimingSeiContext getPicTimingSeiContext() {
				return ctx;
			}

			@Override
			public Locator getLocator() {
				return null;
			}
		};
	}
}
