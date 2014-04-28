package uk.co.badgersinfoil.chunkymonkey.hls;

import java.net.InetAddress;
import uk.co.badgersinfoil.chunkymonkey.Locator;

public class SockLocator implements Locator {

	private InetAddress remoteAddr;
	private Locator parent;

	public SockLocator(InetAddress remote) {
		this.remoteAddr = remote;
		this.parent = null;
	}

	public SockLocator(InetAddress remote, Locator parent) {
		this.remoteAddr = remote;
		this.parent = parent;
	}

	public InetAddress getRemoteAddr() {
		return remoteAddr;
	}

	@Override
	public Locator getParent() {
		return parent;
	}

	@Override
	public String toString() {
		String desc = "Socket: remote address "+remoteAddr.getHostAddress();
		if (parent == null) {
			return desc;
		}
		return desc+"\n  at "+parent.toString();
	}
}
