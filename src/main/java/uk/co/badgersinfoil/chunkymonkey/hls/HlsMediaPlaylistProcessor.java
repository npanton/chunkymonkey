package uk.co.badgersinfoil.chunkymonkey.hls;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import uk.co.badgersinfoil.chunkymonkey.event.Alert;
import uk.co.badgersinfoil.chunkymonkey.event.Perf;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMasterPlaylistProcessor.BadResolutionEvent;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMasterPlaylistProcessor.MissingCodecsEvent;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMasterPlaylistProcessor.UnknownCodecEvent;
import uk.co.badgersinfoil.chunkymonkey.rfc6381.CodecsParser;
import uk.co.badgersinfoil.chunkymonkey.rfc6381.Rfc6381Codec;
import uk.co.badgersinfoil.chunkymonkey.rfc6381.UnknownCodec;
import net.chilicat.m3u8.Element;
import net.chilicat.m3u8.ParseException;
import net.chilicat.m3u8.Playlist;
import net.chilicat.m3u8.PlaylistInfo;

/**
 * Handle the polling for updates of the 'Media Playlist' <code>.m3u8</code>
 * resource.
 */
public class HlsMediaPlaylistProcessor {

	@LogFormat("ETag header is still {etag}, but Last-Modified has changed from {oldLastModified} to {newLastModified}")
	public static class EtagSameLastmodChangedEvent extends Alert { }

	public static class HlsMediaPlaylistLoadPerf extends Perf { }

	private static final Pattern RESOLUTION = Pattern.compile("(\\d+)x(\\d+)");

	private ScheduledExecutorService scheduler;
	private HttpClient httpclient;
	private RequestConfig config = null;
	private Reporter rep = Reporter.NULL;
	private HttpResponseChecker manifestResponseChecker = HttpResponseChecker.NULL;
	private HlsMediaPlaylistConsumer playlistConsumer;
	private CodecsParser codecsParser;

	public HlsMediaPlaylistProcessor(ScheduledExecutorService scheduler,
	                                 HttpClient httpclient,
	                                 HlsMediaPlaylistConsumer playlistConsumer,
	                                 CodecsParser codecsParser)
	{
		this.scheduler = scheduler;
		this.httpclient = httpclient;
		this.playlistConsumer = playlistConsumer;
		this.codecsParser = codecsParser;
	}

	public void setManifestResponseChecker(HttpResponseChecker manifestResponseChecker) {
		this.manifestResponseChecker = manifestResponseChecker;
	}

	private void process(final HlsMediaPlaylistContext ctx, final Playlist playlist, HttpResponse resp) {
		long now = System.currentTimeMillis();
		playlistConsumer.onPlaylistHeader(ctx.getConsumerContext(), playlist);
		ctx.refreshInterval = playlist.getTargetDuration() * 1000;
		if (ctx.firstLoad == 0) {
			ctx.firstLoad = now;
		}
		for (Element e : playlist) {
			playlistConsumer.onPlaylistElement(ctx.getConsumerContext(), e);
		}
		playlistConsumer.onPlaylistEnd(ctx.getConsumerContext());
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
		// try to keep things to the implied schedule, rather than
		// falling behind a little bit, each iteration,
		long adjustment = (now - ctx.firstLoad) % ctx.refreshInterval;
		long delay = ctx.refreshInterval - adjustment;
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

	public void process(HlsMediaPlaylistContext ctx) {
		try {
			requestManifest(ctx);
		} catch (Exception e) {
			rep.carp(ctx.getLocator(), "Loading media manifest failed: %s", e.toString());
			scheduleRetry(ctx);
			return;
		}
		scheduleNextRefresh(ctx, System.currentTimeMillis());
	}

	private void scheduleRetry(final HlsMediaPlaylistContext ctx) {
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
		}, ctx.refreshInterval, TimeUnit.MILLISECONDS);
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
		new HlsMediaPlaylistLoadPerf()
			.with("endState", stat.getEndState())
			.with("durationMillis", stat.getDurationMillis())
			.at(ctx)
			.to(rep);
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

	public void stop(HlsMediaPlaylistContext mctx) {
		mctx.running(false);
		// TODO: cancel any pending Future instances
	}

	public HlsMediaPlaylistContext createContext(HlsMasterPlaylistContext parent, URI manifest, PlaylistInfo playlistInfo) {
		List<Rfc6381Codec> codecList = null;
		Dimension resolution = null;
		if (playlistInfo != null) {
			String codecs = playlistInfo.getCodecs();
			if (codecs == null) {
				new MissingCodecsEvent()
					.with("playlistUri", manifest)
					.at(parent)
					.to(rep);
			} else {
				codecList = codecsParser.parseCodecs(codecs);
				for (Rfc6381Codec c : codecList) {
					if (c instanceof UnknownCodec) {
						new UnknownCodecEvent()
							.with("codec", c)
							.with("playlistUri", manifest)
							.at(parent)
							.to(rep);
					}
				}
			}
			if (playlistInfo.getResolution() != null) {
				Matcher m = RESOLUTION.matcher(playlistInfo.getResolution());
				if (m.matches()) {
					resolution = new Dimension(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
				} else {
					new BadResolutionEvent()
						.with("resolution", playlistInfo.getResolution())
						.at(parent)
						.to(rep);
				}
			}
		}
		HlsMediaPlaylistContext ctx = new HlsMediaPlaylistContext(parent, manifest, playlistInfo, codecList, resolution);
		ctx.setConsumerContext(playlistConsumer.createContext(ctx));
		return ctx;
	}
}
