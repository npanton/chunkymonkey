package uk.co.badgersinfoil.chunkymonkey.hls;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.Reporter.Event;
import uk.co.badgersinfoil.chunkymonkey.Reporter.LogFormat;
import uk.co.badgersinfoil.chunkymonkey.URILocator;
import net.chilicat.m3u8.Element;
import net.chilicat.m3u8.ParseException;
import net.chilicat.m3u8.Playlist;

public class HlsMediaPlaylistProcessor {

	@LogFormat("ETag header is still {etag}, but Last-Modified has changed from {oldLastModified} to {newLastModified}")
	public static class EtagSameLastmodChangedEvent extends Event { }

	private static final long DEFAULT_RETRY_MILLIS = 5000;
	private ScheduledExecutorService scheduler;
	private HttpClient httpclient;
	private RequestConfig config = null;
	private Reporter rep = Reporter.NULL;
	private HttpResponseChecker manifestResponseChecker = HttpResponseChecker.NULL;
	private HlsSegmentProcessor segmentProcessor;

	public HlsMediaPlaylistProcessor(ScheduledExecutorService scheduler,
	                                 HttpClient httpclient,
	                                 HlsSegmentProcessor segmentProcessor)
	{
		this.scheduler = scheduler;
		this.httpclient = httpclient;
		this.segmentProcessor = segmentProcessor;
	}

	public void setManifestResponseChecker(HttpResponseChecker manifestResponseChecker) {
		this.manifestResponseChecker = manifestResponseChecker;
	}

	private void process(final HlsMediaPlaylistContext ctx, final Playlist playlist, HttpResponse resp) {
		long now = System.currentTimeMillis();
		if (ctx.lastMediaSequence != null) {
			int seqEnd = playlist.getMediaSequenceNumber()+playlist.getElements().size()-1;
			if (ctx.haveProcessedMediaSeq(seqEnd)) {
				if (ctx.lastTargetDuration != null) {
					final long missedUpdates = 2; // number of target-durations overdue before warning
					long maxDelayMillis = ctx.lastTargetDuration * 1000 * (1 + missedUpdates + ctx.lastMediaSequenceEndChangeProblems*ctx.lastMediaSequenceEndChangeProblems);
					long delay = now - ctx.lastMediaSequenceEndChange;
					if (delay > maxDelayMillis) {
						List<Header> headers = findAllHeaders(resp, "Date", "Cache-Control", "Age");
						if (headers.isEmpty()) {
							rep.carp(ctx.getLocator(),
							         "No additional segments in %d milliseconds",
							         delay);
						} else {
							rep.carp(ctx.getLocator(),
							         "No additional segments in %d milliseconds; response included: %s",
							         delay,
							         headers);

						}
						ctx.lastMediaSequenceEndChangeProblems++;
					}
				}
			} else {
				ctx.lastMediaSequenceEndChange = now;
				ctx.lastMediaSequenceEndChangeProblems = 0;
			}
		} else {
			ctx.lastMediaSequenceEndChange = now;
			ctx.lastMediaSequenceEndChangeProblems = 0;
		}
		ctx.lastMediaSequence = playlist.getMediaSequenceNumber();
		if (ctx.lastTargetDuration != null && ctx.lastTargetDuration != playlist.getTargetDuration()) {
			rep.carp(ctx.getLocator(), "EXT-X-TARGETDURATION changed? %d became %d", ctx.lastTargetDuration, playlist.getTargetDuration());
		}
		ctx.lastTargetDuration = (long)playlist.getTargetDuration();
		checkDurations(ctx, rep, playlist);
		if (ctx.firstLoad == 0) {
			ctx.firstLoad = now;
		}
		int seq = playlist.getMediaSequenceNumber();
		if (ctx.startup && playlist.getElements().size() > 3) {
			int off = playlist.getElements().size() -3;
			seq += off;
			for (Element e : playlist.getElements().subList(off, off+3)) {
				processPlaylistElement(ctx, seq, e);
				seq++;
			}
		} else {
			for (Element e : playlist) {
				processPlaylistElement(ctx, seq, e);
				seq++;
			}
		}
		ctx.startup = false;
	}

	private static List<Header> findAllHeaders(HttpResponse resp, String... names) {
		List<Header> result = new ArrayList<Header>();
		for (String name : names) {
			Collections.addAll(result, resp.getHeaders(name));
		}
		return result;
	}

	private void scheduleNextRefresh(final HlsMediaPlaylistContext ctx,
			long now) {
		long durationMillis;
		if (ctx.lastTargetDuration == null) {
			durationMillis = DEFAULT_RETRY_MILLIS;
		} else {
			durationMillis = ctx.lastTargetDuration * 1000;
		}
		// try to keep things to the implied schedule, rather than
		// falling behind a little bit, each iteration,
		long adjustment = (now - ctx.firstLoad) % durationMillis;
		long delay = durationMillis - adjustment;
		// scheduleAtFixedRate() would be a good fit were it not that
		// we couldn't handle EXT-X-TARGETDURATION changing,
		trySchedule(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					process(ctx);
				} catch (Throwable e) {
					e.printStackTrace();
				}
				return null;
			}
		}, delay, TimeUnit.MILLISECONDS);
	}

	/**
	 * Ignores any rejection of the attempt to schedule the given callable,
	 * returning null rather than a ScheduledFuture in that case.
	 */
	private <T> ScheduledFuture<T> trySchedule(Callable<T> callable, long delay, TimeUnit unit) {
		try {
			return scheduler.schedule(callable, delay, unit);
		} catch (RejectedExecutionException e) {
			// the scheduler was shut down, so just
			// drop the work on the floor

			return null;
		}
	}

	/**
	 * Report if any element has a duration more than 1 second different
	 * than the playlist's specified EXT-X-TARGETDURATION.
	 */
	private void checkDurations(final HlsMediaPlaylistContext ctx, Reporter rep, Playlist playlist) {
		Element firstProblemElement = null;
		int problemCount = 0;
		int seq = playlist.getMediaSequenceNumber();
		for (Element element : playlist) {
			if (!ctx.haveProcessedMediaSeq(seq)) {
				int diff = element.getDuration() - playlist.getTargetDuration();
				// TODO: HLS spec allows for element durations to be
				//       smaller than the target, but my current use
				//       requires exact agreement -- make this check
				//       configurable / pluggable
				if (diff != 0) {
					if (firstProblemElement == null) {
						firstProblemElement = element;
					}
					problemCount++;
				}
			}
			seq++;
		}
		if (problemCount > 0) {
			rep.carp(ctx.getLocator(), "%d new element(s) with duration different to EXT-X-TARGETDURATION=%d, first being %dsecond duration of %s", problemCount, playlist.getTargetDuration(), firstProblemElement.getDuration(), firstProblemElement.getURI());
		}
	}

	private void processPlaylistElement(final HlsMediaPlaylistContext ctx,
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


	public void process(HlsMediaPlaylistContext ctx) {
		try {
			requestManifest(ctx);
		} catch (Exception e) {
			rep.carp(new URILocator(ctx.manifest), "Loading media manifest failed: %s", e.toString());
			scheduleRetry(ctx);
			return;
		}
		scheduleNextRefresh(ctx, System.currentTimeMillis());
	}

	private void scheduleRetry(final HlsMediaPlaylistContext ctx) {
		long delay = ctx.lastTargetDuration == null ? DEFAULT_RETRY_MILLIS : ctx.lastTargetDuration * 1000 / 2;
		trySchedule(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					process(ctx);
				} catch (Throwable e) {
					e.printStackTrace();
				}
				return null;
			}
		}, delay, TimeUnit.MILLISECONDS);
	}

	private void requestManifest(final HlsMediaPlaylistContext ctx) throws IOException, ParseException {
		HttpGet req = new HttpGet(ctx.manifest);
		ctx.httpCondition.makeConditional(req);
		if (getConfig() != null) {
			req.setConfig(getConfig());
		}
		HttpStat stat = new HttpStat();
		new HttpExecutionWrapper<Void>(rep) {
			@Override
			protected Void handleResponse(HttpClientContext context, CloseableHttpResponse resp, HttpStat stat) throws IOException {
				if (resp.getStatusLine().getStatusCode() == 304) {
					return null;
				}
				manifestResponseChecker.check(ctx, resp, context);
				if (resp.getStatusLine().getStatusCode() != 302) {
					checkCacheValidators(ctx, resp);
				}
				checkAge(ctx, resp);
				InputStream stream = resp.getEntity().getContent();
				try {
					Playlist playlist = Playlist.parse(stream);
					ctx.httpCondition.recordCacheValidators(resp);
					process(ctx, playlist, resp);
				} catch (ParseException e) {
					throw new IOException(e);
				} finally {
					stream.close();
				}
				return null;
			}
		}.execute(httpclient, req, ctx, stat);
		ctx.playlistStats.add(stat);
	}


	private void checkCacheValidators(HlsMediaPlaylistContext ctx,
	                                  CloseableHttpResponse resp)
	{
		// TODO: check should be 'less then or equals'
		boolean lastModMatch = resp.containsHeader("Last-Modified") && resp.getLastHeader("Last-Modified").getValue().equals(ctx.httpCondition.getLastLastModified());
		boolean etagMatch = resp.containsHeader("ETag") && resp.getLastHeader("ETag").getValue().equals(ctx.httpCondition.getLastETag());
		if (lastModMatch != etagMatch) {
			if (lastModMatch) {
				rep.carp(ctx.getLocator(),
				         "Last-Modified header is still %s, but ETag has changed from %s to %s",
				         ctx.httpCondition.getLastLastModified(),
				         ctx.httpCondition.getLastETag(),
				         resp.getLastHeader("ETag").getValue());
			} else {
				new EtagSameLastmodChangedEvent()
					.with("etag", ctx.httpCondition.getLastETag())
					.with("oldLastModified", ctx.httpCondition.getLastLastModified())
					.with("newLastModified", resp.getLastHeader("Last-Modified").getValue())
					.at(ctx)
					.to(rep);
			}
		} else if (lastModMatch) {
			rep.carp(ctx.getLocator(),
			         "Last-Modified header suggests we should have seen a '302 Not Modified', but we got '%s' (this response %s, prev response %s)",
			         resp.getStatusLine(),
			         resp.getLastHeader("Last-Modified").getValue(),
			         ctx.httpCondition.getLastLastModified()
			         );
		} else if (etagMatch) {
			rep.carp(ctx.getLocator(),
			         "ETag header suggests we should have seen a '302 Not Modified', but we got '%s' (this response %s, prev response %s)",
			         resp.getStatusLine(),
			         resp.getLastHeader("ETag").getValue(),
			         ctx.httpCondition.getLastETag()
			         );
		}
	}

	/**
	 * Returns the current request config, or null if defaults are to be
	 * used
	 */
	public RequestConfig getConfig() {
		return config;
	}

	public void setConfig(RequestConfig config) {
		this.config = config;
	}

	public void setReporter(Reporter rep) {
		this.rep = rep;
	}

	private void checkAge(final HlsMediaPlaylistContext ctx,
	                      final CloseableHttpResponse resp)
	{
		if (resp.containsHeader("Age") && ctx.lastTargetDuration != null) {
			try {
				long age = Long.parseLong(resp.getLastHeader("Age").getValue());
				if (age > ctx.lastTargetDuration) {
					rep.carp(ctx.getLocator(), "Response header %s suggests response is stale, given EXT-X-TARGETDURATION=%d", resp.getLastHeader("Age"), ctx.lastTargetDuration);
				}
			} catch (NumberFormatException e) {
				// ignore
			}
		}
	}

	public void stop(HlsMediaPlaylistContext mctx) {
		mctx.running(false);
		// TODO: cancel any pending Future instances
	}
}
