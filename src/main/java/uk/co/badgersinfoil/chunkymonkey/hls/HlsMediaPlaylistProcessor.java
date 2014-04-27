package uk.co.badgersinfoil.chunkymonkey.hls;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.URILocator;
import net.chilicat.m3u8.Element;
import net.chilicat.m3u8.ParseException;
import net.chilicat.m3u8.Playlist;

public class HlsMediaPlaylistProcessor {

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

	public void process(final HlsMediaPlaylistContext ctx, final Playlist playlist) {
		Locator loc = new URILocator(ctx.manifest);
		if (ctx.lastMediaSequence != null) {
			if (ctx.lastMediaSequence > playlist.getMediaSequenceNumber()) {
//				rep.carp(loc, "EXT-X-MEDIA-SEQUENCE went backwards %d to %d", ctx.lastMediaSequence, playlist.getMediaSequenceNumber());
			}

			int seqEnd = playlist.getMediaSequenceNumber()+playlist.getElements().size()-1;
			if (ctx.haveProcessedMediaSeq(seqEnd)) {
				if (ctx.lastTargetDuration != null) {
					long maxDelayMillis = ctx.lastTargetDuration * 1000 * 2;
					long delay = System.currentTimeMillis() - ctx.lastMediaSequenceEndChange;
					if (delay > maxDelayMillis) {
						rep.carp(loc, "No additional segments in %d milliseconds", delay);
					}
				}
			} else {
				ctx.lastMediaSequenceEndChange = System.currentTimeMillis();
			}
		}
		ctx.lastMediaSequence = playlist.getMediaSequenceNumber();
		if (ctx.lastTargetDuration != null && ctx.lastTargetDuration != playlist.getTargetDuration()) {
			rep.carp(loc, "EXT-X-TARGETDURATION changed? %d became %d", ctx.lastTargetDuration, playlist.getTargetDuration());
		}
		ctx.lastTargetDuration = (long)playlist.getTargetDuration();
		long delay = ctx.lastTargetDuration * 1000 / 2;
		scheduler.schedule(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
//System.err.println("refreshing "+ctx.manifest);
				try {
					process(ctx);
				} catch (Throwable e) {
					e.printStackTrace();
				}
				return null;
			}
		}, delay, TimeUnit.MILLISECONDS);
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

	private void processPlaylistElement(final HlsMediaPlaylistContext ctx,
	                                    final int seq,
	                                    final Element e)
	{
		if (!ctx.haveProcessedMediaSeq(seq)) {
			scheduler.submit(new Callable<Void>() {
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
		Playlist playlist;
		try {
			playlist = requestManifest(ctx);
		} catch (Exception e) {
			rep.carp(new URILocator(ctx.manifest), "Loading media manifest failed: %s", e.toString());
			scheduleRetry(ctx);
			return;
		}
		process(ctx, playlist);
	}

	private void scheduleRetry(final HlsMediaPlaylistContext ctx) {
		long delay = ctx.lastTargetDuration == null ? DEFAULT_RETRY_MILLIS : ctx.lastTargetDuration * 1000 / 2;
		scheduler.schedule(new Callable<Void>() {
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

	public Playlist requestManifest(final HlsMediaPlaylistContext ctx) throws IOException, ParseException {
		HttpGet req = new HttpGet(ctx.manifest);
		if (getConfig() != null) {
			req.setConfig(getConfig());
		}
		CloseableHttpResponse resp = (CloseableHttpResponse)httpclient.execute(req);
		URILocator loc = new URILocator(ctx.manifest);
		if (resp.getStatusLine().getStatusCode() != 200) {
			rep.carp(loc, "Request failed %d %s - headers: %s", resp.getStatusLine().getStatusCode(), resp.getStatusLine().getReasonPhrase(), Arrays.toString(resp.getAllHeaders()));
			return null;
		}
		manifestResponseChecker.check(loc, resp);
		InputStream stream = resp.getEntity().getContent();
		try {
			return Playlist.parse(stream);
		} finally {
			stream.close();
			resp.close();
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
}
