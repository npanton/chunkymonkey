package uk.co.badgersinfoil.chunkymonkey.conformist.redundancy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMasterPlaylistContext;

public class HlsRedundantStreamContext implements MediaContext {

	private List<HlsMasterPlaylistContext> streams = new ArrayList<HlsMasterPlaylistContext>(2);
	ScheduledFuture<Void> checkFuture;
	private MediaContext parent;

	public HlsRedundantStreamContext(MediaContext parent) {
		this.parent = parent;
	}

	public void addStream(HlsMasterPlaylistContext masterPlaylistContext) {
		streams.add(masterPlaylistContext);
	}

	public Iterable<HlsMasterPlaylistContext> streams() {
		return Collections.unmodifiableList(streams);
	}

	@Override
	public Locator getLocator() {
		return parent.getLocator();
	}
}
