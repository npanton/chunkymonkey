package uk.co.badgersinfoil.chunkymonkey.hls;

import net.chilicat.m3u8.Element;

public class HlsMediaPlaylistConsumer {
	private HlsSegmentProcessor segmentProcessor;

	public HlsMediaPlaylistConsumer(HlsSegmentProcessor segmentProcessor) {
		this.segmentProcessor = segmentProcessor;
	}

	public void processPlaylistElement(final HlsMediaPlaylistContext ctx,
	                                   final int seq,
	                                   final Element e)
	{
		if (ctx.running() && !ctx.haveProcessedMediaSeq(seq)) {
			segmentProcessor.scheduleSegment(ctx, seq, e);
			ctx.lastProcessedMediaSeq(seq);
		}
	}
}
