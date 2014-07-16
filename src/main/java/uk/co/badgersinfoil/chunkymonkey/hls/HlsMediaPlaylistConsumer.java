package uk.co.badgersinfoil.chunkymonkey.hls;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import net.chilicat.m3u8.Element;

public class HlsMediaPlaylistConsumer {
	private ScheduledExecutorService scheduler;
	private HlsSegmentProcessor segmentProcessor;

	public HlsMediaPlaylistConsumer(ScheduledExecutorService scheduler, HlsSegmentProcessor segmentProcessor) {
		this.scheduler = scheduler;
		this.segmentProcessor = segmentProcessor;
	}

	public void processPlaylistElement(final HlsMediaPlaylistContext ctx,
	                                   final int seq,
	                                   final Element e)
	{
		if (ctx.running() && !ctx.haveProcessedMediaSeq(seq)) {
			Future<Void> segmentFuture = scheduler.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					try {
						segmentProcessor.processSegment(ctx, seq, e);
					} catch (Throwable e) {
						e.printStackTrace();
					}
					return null;
				}
			});
			ctx.lastProcessedMediaSeq(seq);
		}
	}
}
