package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.HashMap;
import java.util.Map;

import uk.co.badgersinfoil.chunkymonkey.Reporter;

public class PIDFilterPacketConsumer implements TSPacketConsumer {
	private Map<Integer, FilterEntry> map = new HashMap<>();
	private Reporter rep = Reporter.NULL;
	
	public static class FilterEntry {
		private TSPacketConsumer consumer;
		private TSContext context;
		public FilterEntry(TSPacketConsumer consumer, TSContext context) {
			this.consumer = consumer;
			this.context = context;
		}
		public TSPacketConsumer getConsumer() {
			return consumer;
		}
		public TSContext getContext() {
			return context;
		}
	}

	public PIDFilterPacketConsumer() {
	}
	public PIDFilterPacketConsumer(Reporter rep) {
		this.rep = rep;
	}
	
	public PIDFilterPacketConsumer filter(int pid, FilterEntry entry) {
		map.put(pid, entry);
		return this;
	}
	
	public void setReporter(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void packet(TSContext ctx, TSPacket packet) {
		FilterEntry entry = map.get(packet.PID());
		if (entry == null) {
			rep.carp(packet.getLocator(), "Unhandled PID: %d", packet.PID());
			filter(packet.PID(), new FilterEntry(TSPacketConsumer.NULL, null));
		} else {
			TSPacketConsumer consumer = entry.getConsumer();
			consumer.packet(entry.getContext(), packet);
		}
	}
	public FilterEntry getCurrent(int elementryPID) {
		return map.get(elementryPID);
	}

	@Override
	public void end(TSContext context) {
		// TODO: don't think anything is required
		// can we remove the need to provide this method implementation?
	}
}
