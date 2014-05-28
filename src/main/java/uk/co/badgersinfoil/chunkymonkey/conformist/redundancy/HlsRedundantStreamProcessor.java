package uk.co.badgersinfoil.chunkymonkey.conformist.redundancy;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.chilicat.m3u8.Element;
import net.chilicat.m3u8.PlaylistInfo;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.URILocator;
import uk.co.badgersinfoil.chunkymonkey.conformist.redundancy.MasterPlaylistComparator.PlaylistComparisonResult;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMasterPlaylistContext;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMasterPlaylistProcessor;

public class HlsRedundantStreamProcessor {
	private ScheduledExecutorService scheduler;
	private HlsMasterPlaylistProcessor masterPlaylistProcessor;
	private MasterPlaylistComparator masterPlaylistComparator = new MasterPlaylistComparator();
	private Reporter rep;

	public HlsRedundantStreamProcessor(ScheduledExecutorService scheduler,
	                                   HlsMasterPlaylistProcessor masterPlaylistProcessor,
	                                   Reporter rep)
	{
		this.scheduler = scheduler;
		this.masterPlaylistProcessor = masterPlaylistProcessor;
		this.rep = rep;
	}

	public HlsRedundantStreamContext createContext(URI... manifests) {
		if (manifests.length < 2) {
			throw new IllegalArgumentException("expected at least 2 manifests, got " + manifests.length);
		}
		HlsRedundantStreamContext ctx = new HlsRedundantStreamContext();
		for (URI uri : manifests) {
			ctx.addStream(masterPlaylistProcessor.createContext(uri));
		}
		return ctx;
	}

	public void start(HlsRedundantStreamContext ctx) {
		for (HlsMasterPlaylistContext s : ctx.streams()) {
			masterPlaylistProcessor.start(s);
		}
		scheduleInitialCheck(ctx);
	}

	private void scheduleInitialCheck(final HlsRedundantStreamContext ctx) {
		ctx.checkFuture = scheduler.schedule(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				consistencyCheck(ctx);
				return null;
			}
		}, 1, TimeUnit.SECONDS);
	}

	protected void consistencyCheck(HlsRedundantStreamContext ctx) {
		HlsMasterPlaylistContext playlistStream = null;
		for (HlsMasterPlaylistContext s : ctx.streams()) {
			if (playlistStream == null) {
				if (s.lastTopLevel != null) {
					playlistStream = s;
				}
				continue;
			}
			if (s.lastTopLevel != null) {
				consistencyCheckSubstreams(ctx, playlistStream, s);
			}
		}
		// TODO: no need to check every second once we know on what
		//       schedule manifests will be reloaded (register
		//       callbacks for changes?)
		scheduleInitialCheck(ctx);
	}

	private void consistencyCheckSubstreams(HlsRedundantStreamContext ctx,
	                                        HlsMasterPlaylistContext stream1,
	                                        HlsMasterPlaylistContext stream2)
	{
		PlaylistComparisonResult res = masterPlaylistComparator.compare(ctx, stream1.lastTopLevel, stream2.lastTopLevel);
		if (!res.getIn1Only().isEmpty()) {
			rep.carp(new URILocator(stream1.getManifestLocation()), "Master manifest entries not specified by partner: %s", playlistInfos(res.getIn1Only()));
		}
		if (!res.getIn2Only().isEmpty()) {
			rep.carp(new URILocator(stream2.getManifestLocation()), "Master manifest entries not specified by partner: %s", playlistInfos(res.getIn2Only()));
		}

	}

	private Object playlistInfos(List<Element> elements) {
		List<PlaylistInfo> result = new ArrayList<>(elements.size());
		for (Element element : elements) {
			result.add(element.getPlayListInfo());
		}
		return result;
	}

	public void stop(HlsRedundantStreamContext ctx) {
		for (HlsMasterPlaylistContext s : ctx.streams()) {
			masterPlaylistProcessor.stop(s);
		}
		stopChecks(ctx);
	}

	private void stopChecks(HlsRedundantStreamContext ctx) {
		if (ctx.checkFuture != null) {
			ctx.checkFuture.cancel(true);
		}
	}
}
