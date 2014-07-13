package uk.co.badgersinfoil.chunkymonkey.hls;

import java.net.URI;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.event.URILocator;

public class SegmentLocator extends URILocator {

	private int mediaSequence;

	public SegmentLocator(int mediaSequence, URI uri, Locator parent) {
		super(uri, parent);
		this.mediaSequence = mediaSequence;
	}

	public SegmentLocator(int mediaSequence, URI uri) {
		super(uri);
		this.mediaSequence = mediaSequence;
	}

	public int getMediaSequence() {
		return mediaSequence;
	}

	@Override
	public String toString() {
		if (getParent() == null) {
			return "Segment "+mediaSequence+" "+getUri().toString();
		}
		return "Segment "+mediaSequence+" "+getUri().toString()+"\n  at "+getParent().toString();
	}
}
