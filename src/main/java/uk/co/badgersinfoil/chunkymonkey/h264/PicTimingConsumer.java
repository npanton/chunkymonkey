package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.h264.PicTimingSeiConsumer.PicTimingSeiContext;

public interface PicTimingConsumer {

	public static interface PicTimingContext extends MediaContext {
		PicTimingSeiContext getPicTimingSeiContext();
	}

	void picTiming(PicTimingContext ctx, PicTimingHeader picTiming);

	public PicTimingContext createContext(PicTimingSeiContext ctx);
}
