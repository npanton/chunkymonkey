package uk.co.badgersinfoil.chunkymonkey.hls;

import java.net.URI;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import net.chilicat.m3u8.Element;

public class HlsMediaPlaylistConsumer {

	public static class HlsMediaPlaylistConsumerContext implements MediaContext {

		private HlsMediaPlaylistContext parent;

		public HlsMediaPlaylistConsumerContext(HlsMediaPlaylistContext parent) {
			this.parent = parent;
		}

		@Override
		public Locator getLocator() {
			return parent.getLocator();
		}
	}

	private HlsSegmentProcessor segmentProcessor;

	public HlsMediaPlaylistConsumer(HlsSegmentProcessor segmentProcessor) {
		this.segmentProcessor = segmentProcessor;
	}

	public void processPlaylistElement(final HlsMediaPlaylistConsumerContext ctx,
	                                   final int seq,
	                                   final Element e)
	{
		if (ctx.parent.running() && !ctx.parent.haveProcessedMediaSeq(seq)) {
			URI elementUri = ctx.parent.manifest.resolve(e.getURI());
			segmentProcessor.scheduleSegment(ctx.parent, seq, elementUri, e);
			ctx.parent.lastProcessedMediaSeq(seq);
		}
	}

	public HlsMediaPlaylistConsumerContext createContext(HlsMediaPlaylistContext parent) {
		return new HlsMediaPlaylistConsumerContext(parent);
	}
}
