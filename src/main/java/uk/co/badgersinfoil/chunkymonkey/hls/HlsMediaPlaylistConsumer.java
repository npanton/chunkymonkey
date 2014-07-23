package uk.co.badgersinfoil.chunkymonkey.hls;

import java.net.URI;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Alert;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;
import net.chilicat.m3u8.Element;
import net.chilicat.m3u8.Playlist;

public class HlsMediaPlaylistConsumer {

	private static enum State { STARTING, STARTED }

	@LogFormat("EXT-X-TARGETDURATION changed? {oldTargetDuration} became {newTargetDuration}")
	public static class TargetDurationChangedEvent extends Alert { }
	@LogFormat("Element duration {elementDuration} does not match media manifest EXT-X-TARGETDURATION={targetDuration}")
	public static class ElementDurationMismatchEvent extends Alert { }

	public static class HlsMediaPlaylistConsumerContext implements MediaContext {

		private HlsMediaPlaylistContext parent;
		private AtomicLong lastProcessedMediaSeq = new AtomicLong();
		private State state = State.STARTING;
		Queue<Element> startupSegments = new LinkedList<>();
		public Long lastTargetDuration = null;

		public HlsMediaPlaylistConsumerContext(HlsMediaPlaylistContext parent) {
			this.parent = parent;
		}

		@Override
		public Locator getLocator() {
			return parent.getLocator();
		}

		public boolean haveProcessedMediaSeq(long seq) {
			return lastProcessedMediaSeq() >= seq;
		}
		public void lastProcessedMediaSeq(long seq) {
			lastProcessedMediaSeq.set(seq);
		}
		public long lastProcessedMediaSeq() {
			return lastProcessedMediaSeq.get();
		}
		void setState(State state) {
			this.state = state;
		}
		State getState() {
			return state;
		}
	}

	private HlsSegmentProcessor segmentProcessor;
	private Reporter rep = Reporter.NULL;

	public HlsMediaPlaylistConsumer(HlsSegmentProcessor segmentProcessor) {
		this.segmentProcessor = segmentProcessor;
	}

	public void setReporter(Reporter rep) {
		this.rep = rep;
	}

	public void onPlaylistHeader(final HlsMediaPlaylistConsumerContext ctx, final Playlist playlist) {
		if (ctx.lastTargetDuration != null && ctx.lastTargetDuration != playlist.getTargetDuration()) {
			new TargetDurationChangedEvent()
				.with("oldTargetDuration", ctx.lastTargetDuration)
				.with("newTargetDuration", playlist.getTargetDuration())
				.at(ctx)
				.to(rep);
		}
		ctx.lastTargetDuration = (long)playlist.getTargetDuration();
	}

	public void onPlaylistElement(final HlsMediaPlaylistConsumerContext ctx,
	                              final Element e)
	{
		// Report if any element has a duration more than 1 second different
		// than the playlist's specified EXT-X-TARGETDURATION.
		if (ctx.lastTargetDuration != null && ctx.lastTargetDuration != e.getDuration()) {
			new ElementDurationMismatchEvent()
				.with("oldTargetDuration", ctx.lastTargetDuration)
				.with("newTargetDuration", e.getDuration())
				.at(ctx)
				.to(rep);
		}
		if (ctx.state == State.STARTING) {
			ctx.startupSegments.add(e);
			if (ctx.startupSegments.size() > 3) {
				ctx.startupSegments.remove();
			}
		} else {
			if (ctx.parent.running() && !ctx.haveProcessedMediaSeq(e.getMediaSequence())) {
				schedule(ctx, e);
			}
		}
	}

	private void schedule(final HlsMediaPlaylistConsumerContext ctx,
	                      final Element e)
	{
		URI elementUri = ctx.parent.manifest.resolve(e.getURI());
		segmentProcessor.scheduleSegment(ctx.parent, elementUri, e);
		ctx.lastProcessedMediaSeq(e.getMediaSequence());
	}

	public void onPlaylistEnd(final HlsMediaPlaylistConsumerContext ctx) {
		if (ctx.state == State.STARTING) {
			for (Element e : ctx.startupSegments) {
				schedule(ctx, e);
			}
			ctx.state = State.STARTED;
		}
	}

	public HlsMediaPlaylistConsumerContext createContext(HlsMediaPlaylistContext parent) {
		return new HlsMediaPlaylistConsumerContext(parent);
	}
}
