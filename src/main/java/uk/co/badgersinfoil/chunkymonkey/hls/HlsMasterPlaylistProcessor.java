package uk.co.badgersinfoil.chunkymonkey.hls;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.chilicat.m3u8.Element;
import net.chilicat.m3u8.ParseException;
import net.chilicat.m3u8.Playlist;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Alert;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;

public class HlsMasterPlaylistProcessor {

	@LogFormat("Missing CODECS header info for entry {playlistUri}")
	public static class MissingCodecsEvent extends Alert {}
	@LogFormat("Unknown codec {codec} for media playlist {playlistUri}")
	public static class UnknownCodecEvent extends Alert {}
	@LogFormat("Bad RESOLUTION {resolution}")
	public static class BadResolutionEvent extends Alert {}
	@LogFormat("Followed {redirectCount} redirect(s) to: {finalUri}")
	public static class RedirectionEvent extends Alert {}

	public static class MasterManifestLocator implements Locator {

		private Locator parent;
		private URI uri;

		public MasterManifestLocator(Locator parent, URI uri) {
			this.parent = parent;
			this.uri = uri;
		}

		public URI getUri() {
			return uri;
		}

		@Override
		public Locator getParent() {
			return parent;
		}
		public String toString() {
			if (parent == null) {
				return "Master manifest "+uri.toString();
			}
			return "Master manifest "+uri.toString()+"\n  at "+parent.toString();
		}
	}

	private ScheduledExecutorService scheduler;
	private HttpClient httpclient;
	private HlsMediaPlaylistProcessor mediaPlaylistProcessor;
	private HttpResponseChecker responseChecker = HttpResponseChecker.NULL;
	private Reporter rep = Reporter.NULL;
	private RequestConfig config;

	public HlsMasterPlaylistProcessor(ScheduledExecutorService scheduler, HttpClient httpclient, HlsMediaPlaylistProcessor mediaPlaylistProcessor) {
		this.scheduler = scheduler;
		this.httpclient = httpclient;
		this.mediaPlaylistProcessor = mediaPlaylistProcessor;
	}

	public void setReporter(Reporter rep) {
		this.rep = rep;
	}

	public HlsMasterPlaylistContext createContext(MediaContext parent, URI manifest) {
		return new HlsMasterPlaylistContext(parent, manifest);
	}

	public void setResponseChecker(HttpResponseChecker responseChecker) {
		this.responseChecker = responseChecker;
	}

	public void start(final HlsMasterPlaylistContext ctx) {
		ctx.running(true);
		ctx.topLevelManifestFuture = scheduler.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					loadTopLevelManifest(ctx);
				} catch (Throwable t) {
					t.printStackTrace();
				}
				return null;
			}
		});
	}

	private void loadTopLevelManifest(final HlsMasterPlaylistContext ctx) {
		Playlist playlist = null;
		try {
			playlist = requestManifest(ctx);
		} catch (Exception e) {
			System.err.println("Loading top level manifest failed.  Will try again in 1 minute.");
			e.printStackTrace();
		}
		if (playlist == null) {
			ctx.topLevelManifestFuture = trySchedule(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					try {
						loadTopLevelManifest(ctx);
					} catch (Throwable e) {
						e.printStackTrace();
					}
					return null;
				}
			}, 1, TimeUnit.MINUTES);
			return;
		}
		// TODO: handle the unlikely case that the kind of
		//       manifest changes between refreshes
		if (isMasterPlaylist(playlist)) {
			processMasterPlaylistEntries(ctx, playlist);
			ctx.lastTopLevel = playlist;
			// reload in 5 mins in case it changes
			ctx.topLevelManifestFuture = trySchedule(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					try {
						loadTopLevelManifest(ctx);
					} catch (Throwable e) {
						e.printStackTrace();
					}
					return null;
				}
			}, 5, TimeUnit.MINUTES);
		} else {
			// this is not a real top-level manifest, but
			// as a special case handle it anyway,
			HlsMediaPlaylistContext mediaCtx = mediaPlaylistProcessor.createContext(ctx, ctx.getManifestLocation(), null);
			ctx.mediaContexts.put(ctx.getManifestLocation(), mediaCtx);
			// TODO: making a second request for the same manifest now is inefficient,
			mediaPlaylistProcessor.process(mediaCtx);
		}
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

	private void processMasterPlaylistEntries(HlsMasterPlaylistContext ctx,
	                                          Playlist playlist)
	{
		//if (ctx.lastTopLevel != null) {
		//	TODO: account for and changes
		//}
		for (Element e : playlist) {
			if (!ctx.mediaContexts.containsKey(e.getURI())) {
				addMediaPlaylist(ctx, e);
			}
		}
	}

	private void addMediaPlaylist(HlsMasterPlaylistContext ctx, Element e) {
		HlsMediaPlaylistContext mediaCtx = mediaPlaylistProcessor.createContext(ctx, ctx.getManifestLocation().resolve(e.getURI()), e.getPlayListInfo());
		ctx.mediaContexts.put(e.getURI(), mediaCtx);
		mediaPlaylistProcessor.process(mediaCtx);
	}

	private boolean isMasterPlaylist(Playlist playList) {
		return playList.getElements().get(0).isPlayList();
	}

	public Playlist requestManifest(final HlsMasterPlaylistContext ctx) throws IOException, ParseException {
		HttpGet req = new HttpGet(ctx.getManifestLocation());
		ctx.httpCondition.makeConditional(req);
		if (config != null) {
			req.setConfig(config);
		}
		HttpStat stat = new HttpStat();
		return new HttpExecutionWrapper<Playlist>(rep) {
			@Override
			protected Playlist handleResponse(HttpClientContext context, CloseableHttpResponse resp, HttpStat stat) throws IOException {
				if (resp.getStatusLine().getStatusCode() == 304) {
					return null;
				}
				List<URI> redirects = context.getRedirectLocations();
				if (redirects != null && !redirects.isEmpty()) {
					URI finalUri = null;
					try {
						finalUri = URIUtils.resolve(ctx.getManifestSpecified(), context.getTargetHost(), redirects);
						ctx.setManifestRedirectLocation(finalUri);
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
					new RedirectionEvent()
						.with("redirectCount", redirects.size())
						.with("finalUri", finalUri==null ? redirects.get(redirects.size()-1) : finalUri)
						.at(ctx)
						.to(rep);
				}
				responseChecker.check(ctx, resp, context);
				ctx.lastUpdated = System.currentTimeMillis();
				InputStream stream = resp.getEntity().getContent();
				try {
					// TODO: call a handler, rather than
					//       returning a value
					Playlist p = Playlist.parse(stream);
					ctx.httpCondition.recordCacheValidators(resp);
					return p;
				} catch (ParseException e) {
					throw new IOException(e);
				} finally {
					stream.close();
				}
			}
		}.execute(httpclient, req, ctx, stat );
	}

	public void stop(final HlsMasterPlaylistContext ctx) {
		ctx.running(false);
		ctx.topLevelManifestFuture.cancel(true);
		for (HlsMediaPlaylistContext mctx : ctx.mediaContexts.values()) {
			mediaPlaylistProcessor.stop(mctx);
		}
	}

	public void setConfig(RequestConfig config) {
		this.config = config;
	}
}
