package uk.co.badgersinfoil.chunkymonkey.ts;

import uk.co.badgersinfoil.chunkymonkey.event.Locator;

public class TSPacketLocator implements Locator {

	private long packetNo;
	private Locator parent;

	public TSPacketLocator(Locator parent, long packetNo) {
		this.parent = parent;
		this.packetNo = packetNo;
	}

	public long getPacketNo() {
		return packetNo;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("TS Packet#").append(packetNo);
		Locator parent = getParent();
		if (parent != null) {
			b.append("\n  at ");
			b.append(parent);
		}
		return b.toString();
	}

	@Override
	public Locator getParent() {
		return parent;
	}
}