package uk.co.badgersinfoil.chunkymonkey.conformist.redundancy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMasterPlaylistContext;

public class HlsRedundantStreamContext {

	private List<HlsMasterPlaylistContext> streams = new ArrayList<HlsMasterPlaylistContext>(2);
	ScheduledFuture<Void> checkFuture;

	public void addStream(HlsMasterPlaylistContext masterPlaylistContext) {
		streams.add(masterPlaylistContext);
	}

	public Iterable<HlsMasterPlaylistContext> streams() {
		return Collections.unmodifiableList(streams);
	}
}
