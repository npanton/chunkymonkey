package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import io.netty.buffer.ByteBuf;

public interface PESConsumer {

	PESConsumer NULL = new PESConsumer() {
		@Override
		public void start(ElementryContext ctx, PESPacket pesPacket) { }
		@Override
		public void continuation(ElementryContext ctx, TSPacket packet, ByteBuf payload) { }
		@Override
		public void end(ElementryContext ctx) { }
		@Override
		public void continuityError(ElementryContext ctx) { }
		@Override
		public ElementryContext createContext() {
			return null;
		}
	};

	public static class MultiPesConsumer implements PESConsumer {
		public static class MultiElementryContext implements ElementryContext {
			private List<Entry> list = new ArrayList<>();
			public MultiElementryContext(List<PESConsumer> list) {
				for (PESConsumer p : list) {
					Entry e = new Entry();
					e.consumer = p;
					e.ctx = p.createContext();
					this.list.add(e);
				}
			}
			public List<ElementryContext> getContexts() {
				List<ElementryContext> result = new ArrayList<ElementryContext>();
				for (Entry e : list) {
					result.add(e.ctx);
				}
				return result;
			}
		}
		private static class Entry {
			public PESConsumer consumer;
			public ElementryContext ctx;
		}
		private List<PESConsumer> list = new ArrayList<PESConsumer>();

		public MultiPesConsumer(PESConsumer... list) {
			Collections.addAll(this.list, list);
		}

		@Override
		public void start(ElementryContext ctx, PESPacket pesPacket) {
			MultiElementryContext mCtx = (MultiElementryContext)ctx;
			for (Entry e : mCtx.list) {
				e.consumer.start(e.ctx, pesPacket);
			}
		}

		@Override
		public void continuation(ElementryContext ctx, TSPacket packet, ByteBuf payload) {
			MultiElementryContext mCtx = (MultiElementryContext)ctx;
			for (Entry e : mCtx.list) {
				e.consumer.continuation(e.ctx, packet, payload);
			}
		}

		@Override
		public void end(ElementryContext ctx) {
			MultiElementryContext mCtx = (MultiElementryContext)ctx;
			for (Entry e : mCtx.list) {
				e.consumer.end(e.ctx);
			}
		}

		@Override
		public void continuityError(ElementryContext ctx) {
			MultiElementryContext mCtx = (MultiElementryContext)ctx;
			for (Entry e : mCtx.list) {
				e.consumer.continuityError(e.ctx);
			}
		}

		@Override
		public ElementryContext createContext() {
			return new MultiElementryContext(list);
		}
	}

	void start(ElementryContext ctx, PESPacket pesPacket);

	void continuation(ElementryContext ctx, TSPacket packet, ByteBuf payload);

	void end(ElementryContext ctx);

	ElementryContext createContext();

	void continuityError(ElementryContext ctx);

}
