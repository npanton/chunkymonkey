package uk.co.badgersinfoil.chunkymonkey.hls;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import net.chilicat.m3u8.PlaylistInfo;

public class HlsMediaPlaylistContext {

	private HlsMasterPlaylistContext ctx;
	URI manifest;
	public Long lastTargetDuration = null;
	public Integer lastMediaSequence = null;
	private AtomicInteger lastProcessedMediaSeq = new AtomicInteger();
	public boolean startup = true;
	public long lastMediaSequenceEndChange;
	private PlaylistInfo playlistInfo;

	public HlsMediaPlaylistContext(HlsMasterPlaylistContext ctx,
	                               URI manifest,
	                               PlaylistInfo playlistInfo)
	{
		this.ctx = ctx;
		this.manifest = manifest;
		this.playlistInfo = playlistInfo;
	}

	public boolean haveProcessedMediaSeq(int seq) {
		return lastProcessedMediaSeq.get() >= seq;
	}

	public void lastProcessedMediaSeq(int seq) {
		lastProcessedMediaSeq.set(seq);
	}
}
