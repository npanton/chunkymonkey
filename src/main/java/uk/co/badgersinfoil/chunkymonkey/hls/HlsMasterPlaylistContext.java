package uk.co.badgersinfoil.chunkymonkey.hls;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import net.chilicat.m3u8.Playlist;

public class HlsMasterPlaylistContext implements MediaContext {

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

	private MediaContext parent;
	private URI manifest;
	public Future<Void> topLevelManifestFuture;
	public Playlist lastTopLevel;
	public Map<URI, HlsMediaPlaylistContext> mediaContexts = new HashMap<>();
	private URI manifestRedirectLocation;
	public long lastUpdated;
	public HttpCondition httpCondition = new HttpCondition();
	private AtomicBoolean running = new AtomicBoolean(true);

	public HlsMasterPlaylistContext(MediaContext parent, URI manifest) {
		this.parent = parent;
		this.manifest = manifest;
	}

	public URI getManifestSpecified() {
		return manifest;
	}

	/**
	 * Returns the URI of the manifest that was requested, after any
	 * redirections, or the same value as {@link #getManifestSpecified()}
	 * if no redirections occurred during the last retrieval of the
	 * manifest.
	 */
	public URI getManifestLocation() {
		return manifestRedirectLocation == null
		       ? manifest
		       : manifestRedirectLocation;
	}

	public void setManifestRedirectLocation(URI location) {
		this.manifestRedirectLocation = location;
	}

	@Override
	public Locator getLocator() {
		return new MasterManifestLocator(parent.getLocator(), getManifestLocation());
	}

	public boolean running() {
		return running.get();
	}
	public void running(boolean running) {
		this.running.set(running);
	}
}
