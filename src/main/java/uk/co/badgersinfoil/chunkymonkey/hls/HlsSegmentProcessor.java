package uk.co.badgersinfoil.chunkymonkey.hls;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import net.chilicat.m3u8.Element;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.URILocator;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.TransportStreamParser;

public class HlsSegmentProcessor {

	private static final float MAX_DOWNLOAD_DURATION = 0.8f;  // 80%

	private HttpClient httpclient;
	private Reporter rep = Reporter.NULL;
	private TSPacketConsumer consumer;
	private HttpResponseChecker manifestResponseChecker = HttpResponseChecker.NULL;
	private RequestConfig config;

	public HlsSegmentProcessor(Reporter rep,
	                           HttpClient httpclient,
	                           TSPacketConsumer consumer)
	{
		this.rep = rep;
		this.httpclient = httpclient;
		this.consumer = consumer;
	}

	public void setManifestResponseChecker(HttpResponseChecker manifestResponseChecker) {
		this.manifestResponseChecker = manifestResponseChecker;
	}

	protected void processSegment(final HlsMediaPlaylistContext ctx,
	                              final int seq,
	                              final Element element)
	{
		URI elementUri = ctx.manifest.resolve(element.getURI());
		HttpGet req = new HttpGet(elementUri);
		if (getConfig() != null) {
			req.setConfig(getConfig());
		}
		URILocator manifestLoc = new URILocator(ctx.manifest);
		final Locator loc = new URILocator(elementUri, manifestLoc);
		HttpStat stat = new HttpStat();
		new HttpExecutionWrapper<Void>(rep) {
			@Override
			protected Void handleResponse(HttpClientContext context, CloseableHttpResponse resp, HttpStat stat) throws IOException {
				manifestResponseChecker.check(loc, resp, context);
				InputStream stream = resp.getEntity().getContent();
				TransportStreamParser parser = new TransportStreamParser(loc, consumer);
				parser.parse(stream);
				stat.end();
				long expectedDurationMillis = element.getDuration() * 1000;
				if (stat.getDurationMillis() > expectedDurationMillis * MAX_DOWNLOAD_DURATION) {
					rep.carp(loc, "Took %dms to download, but playback duration is %dms", stat.getDurationMillis(), expectedDurationMillis);
				}
				return null;
			}
		}.execute(httpclient, req, loc, stat);
		ctx.segmentStats.add(stat);
	}

	public void setConfig(RequestConfig config) {
		this.config = config;
	}
	public RequestConfig getConfig() {
		return config;
	}

}
