package uk.co.badgersinfoil.chunkymonkey.hls;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import uk.co.badgersinfoil.chunkymonkey.rfc6381.Rfc6381Codec;
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
	public HttpStats segmentStats = new HttpStats();
	public HttpStats playlistStats = new HttpStats();
	private List<Rfc6381Codec> codecList;

	public HlsMediaPlaylistContext(HlsMasterPlaylistContext ctx,
	                               URI manifest,
	                               PlaylistInfo playlistInfo,
	                               List<Rfc6381Codec> codecList)
	{
		this.ctx = ctx;
		this.manifest = manifest;
		this.playlistInfo = playlistInfo;
		this.codecList = codecList;
	}

	public boolean haveProcessedMediaSeq(int seq) {
		return lastProcessedMediaSeq() >= seq;
	}

	public PlaylistInfo getPlaylistInfo() {
		return playlistInfo;
	}

	public List<Rfc6381Codec> getCodecList() {
		return Collections.unmodifiableList(codecList);
	}

	public void lastProcessedMediaSeq(int seq) {
		lastProcessedMediaSeq.set(seq);
	}
	public int lastProcessedMediaSeq() {
		return lastProcessedMediaSeq.get();
	}
}
