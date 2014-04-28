package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiTSPacketConsumer implements TSPacketConsumer {


	public static class MultiTsContext implements TSContext {
		public static class Entry {
			public Entry(TSContext context,
			             TSPacketConsumer consumer)
			{
				this.context = context;
				this.consumer = consumer;
			}
			TSPacketConsumer consumer;
			private TSContext context;
			public TSContext getContext() {
				return context;
			}
		}
		public List<Entry> list = new ArrayList<>();
		public MultiTsContext(TSContext parent, List<TSPacketConsumer> list) {
			for (TSPacketConsumer consumer : list) {
				this.list.add(new Entry(consumer.createContext(parent), consumer));
			}
		}

	}

	private List<TSPacketConsumer> list = new ArrayList<>();

	public MultiTSPacketConsumer(TSPacketConsumer... consumers) {
		Collections.addAll(list, consumers);
	}

	@Override
	public void packet(TSContext ctx, TSPacket packet) {
		MultiTsContext mctx = (MultiTsContext)ctx;
		for (MultiTsContext.Entry e : mctx.list) {
			e.consumer.packet(e.getContext(), packet);
		}
	}

	@Override
	public void end(TSContext ctx) {
		MultiTsContext mctx = (MultiTsContext)ctx;
		for (MultiTsContext.Entry e : mctx.list) {
			e.consumer.end(e.getContext());
		}
	}

	@Override
	public TSContext createContext(TSContext parent) {
		return new MultiTsContext(parent, list);
	}
}
