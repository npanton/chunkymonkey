package uk.co.badgersinfoil.chunkymonkey.hls;

import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Arrays;
import net.chilicat.m3u8.Element;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.URILocator;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.TransportStreamParser;

public class HlsSegmentProcessor {

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

	protected void processSegment(HlsMediaPlaylistContext ctx,
	                              int seq,
	                              Element element)
	{
		URI elementUri = ctx.manifest.resolve(element.getURI());
//System.out.println("Getting segment "+seq+": " + elementUri);
		HttpGet req = new HttpGet(elementUri);
		if (getConfig() != null) {
			req.setConfig(getConfig());
		}
		try {
			CloseableHttpResponse resp = (CloseableHttpResponse)httpclient.execute(req);
			URILocator manifestLoc = new URILocator(ctx.manifest);
			URILocator loc = new URILocator(elementUri, manifestLoc);
			manifestResponseChecker.check(loc, resp);
			if (resp.getStatusLine().getStatusCode() != 200) {
				rep.carp(new URILocator(elementUri), "Request failed %d %s - headers: %s", resp.getStatusLine().getStatusCode(), resp.getStatusLine().getReasonPhrase(), Arrays.toString(resp.getAllHeaders()));
			} else {
				InputStream stream = resp.getEntity().getContent();
				TransportStreamParser parser = new TransportStreamParser(loc, consumer);
				parser.parse(stream);
			}
			resp.close();
		} catch (SocketTimeoutException e) {
			// TODO: parent Locator
			rep.carp(new URILocator(elementUri), "Loading segment %d failed after %d bytes: %s", seq, e.bytesTransferred, e.getMessage());
		} catch (Exception e) {
			// TODO: parent Locator
			rep.carp(new URILocator(elementUri), "Loading segment %d failed: %s", seq, e.getMessage());
		}
	}

	public void setConfig(RequestConfig config) {
		this.config = config;
	}
	public RequestConfig getConfig() {
		return config;
	}

}
