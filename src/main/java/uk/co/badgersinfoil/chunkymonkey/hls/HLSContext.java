package uk.co.badgersinfoil.chunkymonkey.hls;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.chilicat.m3u8.Element;
import net.chilicat.m3u8.ParseException;
import net.chilicat.m3u8.PlayList;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import uk.co.badgersinfoil.chunkymonkey.URILocator;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.TransportStreamParser;

// TODO: handle top-level manifests

public class HLSContext {

	private URI manifest;
	private CloseableHttpClient httpclient;
	private TSPacketConsumer consumer;

	public HLSContext(URI manifest, TSPacketConsumer consumer) {
		this.manifest = manifest;
		this.consumer = consumer;
	}

	public void setHttpClient(CloseableHttpClient httpclient) {
		this.httpclient = httpclient;
	}

	public PlayList requestManifest() throws IOException, ParseException {
		HttpGet req = new HttpGet(manifest);
		CloseableHttpResponse resp = httpclient.execute(req);
		InputStream stream = resp.getEntity().getContent();
		try {
			return PlayList.parse(stream);
		} finally {
			stream.close();
		}
	}
	
	public void consumeStream() {
		List<Element> backlog = new ArrayList<Element>();
		long startTime = System.currentTimeMillis();
		PlayList lastPlaylist = null;
		while (true) {
			try {
				PlayList playlist = requestManifest();
				for (Element element : playlist) {
					// TODO: efficiency
					if (lastPlaylist!=null && !backlogContains(lastPlaylist.getElements(), element)) {
						backlog.add(element);
					}
				}
				consumeBacklog(backlog);
				lastPlaylist = playlist;
				long nowTime = System.currentTimeMillis();
				long waitTime = (nowTime - startTime) % playlist.getTargetDuration()*1000L;
				Thread.sleep(waitTime);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void consumeBacklog(List<Element> backlog) throws IOException {
		Iterator<Element> i = backlog.iterator();
		while (i.hasNext()) {
			requestElement(i.next());
			i.remove();
		}
	}

	private boolean backlogContains(List<Element> backlog, Element element) {
		for (Element e : backlog) {
			if (e.getURI().equals(element.getURI())) {
				return true;
			}
		}
		return false;
	}

	public void requestElement(Element element) throws IOException {
		URI elementUri = manifest.resolve(element.getURI());
System.out.println(elementUri);
		HttpGet req = new HttpGet(elementUri);
		CloseableHttpResponse resp = httpclient.execute(req);
		InputStream stream = resp.getEntity().getContent();
		URILocator manifestLoc = new URILocator(manifest);
		URILocator loc = new URILocator(elementUri, manifestLoc);
		TransportStreamParser parser = new TransportStreamParser(loc, consumer);
		parser.parse(stream);
	}
}
