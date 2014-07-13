package uk.co.badgersinfoil.chunkymonkey.hls;

import uk.co.badgersinfoil.chunkymonkey.event.Locator;

public class PackagerInstanceLocator implements Locator {

	private Locator parent;
	private String instanceId;

	public PackagerInstanceLocator(String instanceId, Locator parent) {
		this.instanceId = instanceId;
		this.parent = parent;
	}

	@Override
	public Locator getParent() {
		return parent;
	}

	@Override
	public String toString() {
		if (parent == null) {
			return "Packager instance: "+instanceId;
		}
		return "Packager instance: "+instanceId+"\n  at "+parent.toString();
	}
}
