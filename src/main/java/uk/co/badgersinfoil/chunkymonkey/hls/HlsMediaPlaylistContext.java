package uk.co.badgersinfoil.chunkymonkey.hls;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import net.chilicat.m3u8.PlaylistInfo;

public class HlsMediaPlaylistContext {

	private HlsMasterPlaylistContext ctx;
	public URI manifest;
	public Long lastTargetDuration = null;
	public Integer lastMediaSequence = null;
	private AtomicInteger lastProcessedMediaSeq = new AtomicInteger();
	public boolean startup = true;
	public long lastMediaSequenceEndChange;
	private PlaylistInfo playlistInfo;
	public long firstLoad;
	// count of the number of times we reported the playlist failed to
	// update, so we can decrease the frequency of reports over time if
	// the lack-of-updates persists
	public int lastMediaSequenceEndChangeProblems;

	public HlsMediaPlaylistContext(HlsMasterPlaylistContext ctx,
	                               URI manifest,
	                               PlaylistInfo playlistInfo)
	{
		this.ctx = ctx;
		this.manifest = manifest;
		this.playlistInfo = playlistInfo;
	}

	public boolean haveProcessedMediaSeq(int seq) {
		return lastProcessedMediaSeq() >= seq;
	}

	public PlaylistInfo getPlaylistInfo() {
		return playlistInfo;
	}

	public void lastProcessedMediaSeq(int seq) {
		lastProcessedMediaSeq.set(seq);
	}
	public int lastProcessedMediaSeq() {
		return lastProcessedMediaSeq.get();
	}
}
