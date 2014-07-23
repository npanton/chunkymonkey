package uk.co.badgersinfoil.chunkymonkey.hls;

import java.awt.Dimension;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.event.URILocator;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMediaPlaylistConsumer.HlsMediaPlaylistConsumerContext;
import uk.co.badgersinfoil.chunkymonkey.rfc6381.Rfc6381Codec;
import net.chilicat.m3u8.PlaylistInfo;

public class HlsMediaPlaylistContext implements MediaContext {

	private static final long DEFAULT_REFRESH_INTERVAL = 5000;

	private HlsMasterPlaylistContext ctx;
	public URI manifest;
	public long lastMediaSequenceEndChange;
	private PlaylistInfo playlistInfo;
	public long firstLoad;
	// count of the number of times we reported the playlist failed to
	// update, so we can decrease the frequency of reports over time if
	// the lack-of-updates persists
	public int lastMediaSequenceEndChangeProblems;
	public HttpStats segmentStats = new HttpStats();
	public HttpStats playlistStats = new HttpStats();
	private List<Rfc6381Codec> codecList;
	protected HttpCondition httpCondition = new HttpCondition();
	private AtomicBoolean running = new AtomicBoolean(true);
	private Dimension resolution;
	private HlsMediaPlaylistConsumerContext consumerContext;
	public long refreshInterval = DEFAULT_REFRESH_INTERVAL;

	public HlsMediaPlaylistContext(HlsMasterPlaylistContext ctx,
	                               URI manifest,
	                               PlaylistInfo playlistInfo,
	                               List<Rfc6381Codec> codecList,
	                               Dimension resolution)
	{
		this.ctx = ctx;
		this.manifest = manifest;
		this.playlistInfo = playlistInfo;
		this.codecList = codecList;
		this.resolution = resolution;
	}

	public PlaylistInfo getPlaylistInfo() {
		return playlistInfo;
	}

	public List<Rfc6381Codec> getCodecList() {
		return Collections.unmodifiableList(codecList);
	}


	@Override
	public Locator getLocator() {
		return new URILocator(manifest, ctx.getLocator());
	}

	public boolean running() {
		return running.get();
	}
	public void running(boolean running) {
		this.running.set(running);
	}
	public Dimension getResolution() {
		return resolution;
	}

	public void setConsumerContext(HlsMediaPlaylistConsumerContext consumerContext) {
		this.consumerContext = consumerContext;
	}

	public HlsMediaPlaylistConsumerContext getConsumerContext() {
		return consumerContext;
	}
}
