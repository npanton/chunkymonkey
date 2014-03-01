package uk.co.badgersinfoil.chunkymonkey.h264;

public interface PicTimingConsumer {

	void picTiming(H264Context ctx, PicTimingHeader picTiming);
}
