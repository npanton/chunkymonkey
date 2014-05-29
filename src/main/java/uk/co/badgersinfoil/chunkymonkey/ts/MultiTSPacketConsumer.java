package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;

public class MultiTSPacketConsumer implements TSPacketConsumer {


	public static class MultiMediaContext implements MediaContext {
		public static class Entry {
			public Entry(MediaContext context,
			             TSPacketConsumer consumer)
			{
				this.context = context;
				this.consumer = consumer;
			}
			TSPacketConsumer consumer;
			private MediaContext context;
			public MediaContext getContext() {
				return context;
			}
		}
		public List<Entry> list = new ArrayList<>();
		private MediaContext parent;
		public MultiMediaContext(MediaContext parent, List<TSPacketConsumer> list) {
			this.parent = parent;
			for (TSPacketConsumer consumer : list) {
				this.list.add(new Entry(consumer.createContext(parent), consumer));
			}
		}
		@Override
		public Locator getLocator() {
			return parent.getLocator();
		}

	}

	private List<TSPacketConsumer> list = new ArrayList<>();

	public MultiTSPacketConsumer(TSPacketConsumer... consumers) {
		Collections.addAll(list, consumers);
	}

	@Override
	public void packet(MediaContext ctx, TSPacket packet) {
		MultiMediaContext mctx = (MultiMediaContext)ctx;
		for (MultiMediaContext.Entry e : mctx.list) {
			e.consumer.packet(e.getContext(), packet);
		}
	}

	@Override
	public void end(MediaContext ctx) {
		MultiMediaContext mctx = (MultiMediaContext)ctx;
		for (MultiMediaContext.Entry e : mctx.list) {
			e.consumer.end(e.getContext());
		}
	}

	@Override
	public MediaContext createContext(MediaContext parent) {
		return new MultiMediaContext(parent, list);
	}
}
