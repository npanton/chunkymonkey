package uk.co.badgersinfoil.chunkymonkey.hls;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import net.chilicat.m3u8.Element;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Alert;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.event.Perf;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.TransportStreamParser;

public class HlsSegmentProcessor {

	public static class HlsSegmentLocator implements Locator {

		private Locator parent;
		private URI uri;
		private long mediaSequence;

		public HlsSegmentLocator(Locator parent, URI uri, long mediaSequence) {
			this.parent = parent;
			this.uri = uri;
			this.mediaSequence = mediaSequence;
		}

		public URI getUri() {
			return uri;
		}

		public long getMediaSequence() {
			return mediaSequence;
		}

		@Override
		public Locator getParent() {
			return parent;
		}

		@Override
		public String toString() {
			if (parent == null) {
				return uri.toString();
			}
			return "HLS Segment " + mediaSequence + " " + uri.toString() + "\n  at " + parent.toString();
		}
	}

	@LogFormat("Took {actualMillis}ms to download, but playback duration is {playbackMillis}ms")
	public static class SlowDownloadEvent extends Alert {
	}

	@LogFormat("")
	public static class HlsSegmentLoadPerf extends Perf {
	}

	private static final float MAX_DOWNLOAD_DURATION = 0.8f; // 80%

	private ScheduledExecutorService scheduler;
	private HttpClient httpclient;
	private Reporter rep = Reporter.NULL;
	private TSPacketConsumer consumer;
	private HttpResponseChecker manifestResponseChecker = HttpResponseChecker.NULL;
	private RequestConfig config;

	public static class HlsSegmentTsContext implements MediaContext {
		public HlsMediaPlaylistContext ctx;
		private URI elementUri;
		private long mediaSequence;
		protected TransportStreamParser parser;
		protected MediaContext parseCtx;

		public HlsSegmentTsContext(HlsMediaPlaylistContext ctx, URI elementUri, long mediaSequence) {
			this.ctx = ctx;
			this.elementUri = elementUri;
			this.mediaSequence = mediaSequence;
		}

		@Override
		public Locator getLocator() {
			return new HlsSegmentLocator(ctx.getLocator(), elementUri, mediaSequence);
		}
	}

	public HlsSegmentProcessor(ScheduledExecutorService scheduler, Reporter rep, HttpClient httpclient, TSPacketConsumer consumer) {
		this.scheduler = scheduler;
		this.rep = rep;
		this.httpclient = httpclient;
		this.consumer = consumer;
	}

	public void setManifestResponseChecker(HttpResponseChecker manifestResponseChecker) {
		this.manifestResponseChecker = manifestResponseChecker;
	}

	protected void processSegment(final HlsMediaPlaylistContext ctx, final URI elementUri, final Element element) {
		final HlsSegmentTsContext segCtx = new HlsSegmentTsContext(ctx, elementUri, element.getMediaSequence());
		HttpGet req = new HttpGet(elementUri);
		if (getConfig() != null) {
			req.setConfig(getConfig());
		}
		HttpStat stat = new HttpStat();
		new HttpExecutionWrapper<Void>(rep) {
			@Override
			protected Void handleResponse(HttpClientContext context, CloseableHttpResponse resp, HttpStat stat) throws IOException {
				manifestResponseChecker.check(segCtx, resp, context);
				InputStream stream = resp.getEntity().getContent();
				if (segCtx.parser == null) {
					segCtx.parser = new TransportStreamParser(consumer);
					segCtx.parseCtx = segCtx.parser.createContext(segCtx);
					// TODO: } else if (element.isDiscontinuity()) {
					// segCtx.parser.end(segCtx.parseCtx);
				}
				segCtx.parser.parse(segCtx.parseCtx, stream);
				stat.end();
				long expectedDurationMillis = element.getDuration() * 1000;
				if (stat.getDurationMillis() > expectedDurationMillis * MAX_DOWNLOAD_DURATION) {
					new SlowDownloadEvent().with("actualMillis", stat.getDurationMillis()).with("playbackMillis", expectedDurationMillis).at(segCtx).to(rep);
				}
				return null;
			}
		}.execute(httpclient, req, segCtx, stat);
		ctx.segmentStats.add(stat);
		new HlsSegmentLoadPerf().with("endState", stat.getEndState()).with("durationMillis", stat.getDurationMillis()).at(segCtx).to(rep);
	}

	public void setConfig(RequestConfig config) {
		this.config = config;
	}

	public RequestConfig getConfig() {
		return config;
	}

	// TODO public void end(ctx) { ... }

	public void scheduleSegment(final HlsMediaPlaylistContext ctx, final URI elementUri, final Element e) {
		try {
			Future<Void> segmentFuture = scheduler.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					try {
						processSegment(ctx, elementUri, e);
					} catch (Throwable e) {
						e.printStackTrace();
					}
					return null;
				}
			});
		} catch (RejectedExecutionException ex) {
			// assmue that our ScheduledExecutorService is in the
			// process of being shut down, and ignore this
		}
	}

}
