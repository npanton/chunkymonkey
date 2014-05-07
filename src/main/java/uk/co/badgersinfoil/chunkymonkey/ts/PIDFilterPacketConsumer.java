package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import uk.co.badgersinfoil.chunkymonkey.Reporter;

public class PIDFilterPacketConsumer implements TSPacketConsumer {
	private Map<Integer, TSPacketConsumer> defaultFilterMap = new HashMap<>();
	private Reporter rep = Reporter.NULL;

	public static class FilterEntry {
		private TSPacketConsumer consumer;
		private TSContext context;
		public FilterEntry(TSPacketConsumer consumer, TSContext context) {
			if (context == null) {
				throw new IllegalArgumentException("context must not be null");
			}
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

	public PIDFilterPacketConsumer defaultFilter(int pid, TSPacketConsumer consumer) {
		defaultFilterMap.put(pid, consumer);
		return this;
	}

	public PIDFilterPacketConsumer filter(TSContext ctx, int pid, FilterEntry entry) {
		TransportContext tctx = (TransportContext)ctx;
		tctx.filter(pid, entry);
		return this;
	}

	public void setReporter(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void packet(TSContext ctx, TSPacket packet) {
		TransportContext tctx = (TransportContext)ctx;
		FilterEntry entry = tctx.filterForPid(packet.PID());
		if (entry == null) {
			rep.carp(packet.getLocator(), "Unhandled PID: %d", packet.PID());
			TSPacketConsumer consumer = TSPacketConsumer.NULL;
			filter(ctx, packet.PID(), new FilterEntry(consumer, consumer.createContext(tctx)));
		} else {
			TSPacketConsumer consumer = entry.getConsumer();
			consumer.packet(entry.getContext(), packet);
		}
	}
	public FilterEntry getCurrent(TSContext ctx, int elementryPID) {
		TransportContext tctx = (TransportContext)ctx;
		return tctx.filterForPid(elementryPID);
	}

	public TSContext createContext(TSContext parent) {
		TransportContext ctx = new TransportContext();
		for (Entry<Integer, TSPacketConsumer> e : defaultFilterMap.entrySet()) {
			TSPacketConsumer consumer = e.getValue();
			TSContext tctx = consumer.createContext(ctx);
			ctx.filter(e.getKey(), new FilterEntry(consumer, tctx));
		}
		return ctx;
	}

	@Override
	public void end(TSContext ctx) {
		TransportContext tctx = (TransportContext)ctx;
		// TODO: don't think anything is required
		// can we remove the need to provide this method implementation?
	}
}
