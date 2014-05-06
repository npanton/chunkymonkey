package uk.co.badgersinfoil.chunkymonkey.hls;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import net.chilicat.m3u8.Playlist;

public class HlsMasterPlaylistContext {

	private URI manifest;
	public Future<Void> topLevelManifestFuture;
	public Playlist lastTopLevel;
	public Map<URI, HlsMediaPlaylistContext> mediaContexts = new HashMap<>();
	private URI manifestRedirectLocation;
	public long lastUpdated;

	public HlsMasterPlaylistContext(URI manifest) {
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
}
