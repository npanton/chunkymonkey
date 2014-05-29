package uk.co.badgersinfoil.chunkymonkey.adts;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.MediaDuration;

public interface ADTSContext extends MediaContext {
	MediaDuration getDuration();
}
