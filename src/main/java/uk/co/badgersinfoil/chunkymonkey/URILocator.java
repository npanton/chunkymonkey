package uk.co.badgersinfoil.chunkymonkey;

import java.net.URI;

public class URILocator implements Locator {
	
	private URI uri;
	private Locator parent;

	public URILocator(URI uri, Locator parent) {
		this.uri = uri;
		this.parent = parent;
	}

	public URILocator(URI uri) {
		this.uri = uri;
		this.parent = null;
	}

	@Override
	public Locator getParent() {
		return parent;
	}

	public URI getUri() {
		return uri;
	}
	
	@Override
	public String toString() {
		if (parent == null) {
			return uri.toString();
		}
		return uri.toString()+"\n  at "+parent.toString();
	}
}
