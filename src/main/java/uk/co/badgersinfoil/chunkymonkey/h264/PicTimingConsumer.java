package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.h264.PicTimingSeiConsumer.PicTimingSeiContext;
import uk.co.badgersinfoil.chunkymonkey.ts.TSContext;

public interface PicTimingConsumer {

	public static interface PicTimingContext extends TSContext {
		PicTimingSeiContext getPicTimingSeiContext();
	}

	void picTiming(PicTimingContext ctx, PicTimingHeader picTiming);

	public PicTimingContext createContext(PicTimingSeiContext ctx);
}
