package uk.co.badgersinfoil.chunkymonkey.adts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.MediaDuration;

public interface AdtsFrameConsumer {

	void frame(ADTSContext ctx, ADTSFrame adtsframe);

	public class Multi implements AdtsFrameConsumer {
		private static class MultiADTSContext implements ADTSContext {
			private List<Entry> list = new ArrayList<>();
			private MediaContext parentContext;
			public MultiADTSContext(MediaContext parentContext, List<AdtsFrameConsumer> list) {
				this.parentContext = parentContext;
				for (AdtsFrameConsumer p : list) {
					Entry e = new Entry();
					e.consumer = p;
					e.ctx = p.createContext(this);
					this.list.add(e);
				}
			}
			@Override
			public MediaDuration getDuration() {
				for (Entry e : list) {
					MediaDuration d = e.ctx.getDuration();
					if (d != null) {
						return d;
					}
				}
				return null;
			}
			@Override
			public Locator getLocator() {
				return parentContext.getLocator();
			}
		}
		private static class Entry {
			public AdtsFrameConsumer consumer;
			public ADTSContext ctx;
		}

		List<AdtsFrameConsumer> list = new ArrayList<>();

		public Multi(AdtsFrameConsumer... list) {
			this.list = Arrays.asList(list);
		}
		public Multi(List<AdtsFrameConsumer> list) {
			this.list = list;
		}

		@Override
		public void frame(ADTSContext ctx, ADTSFrame adtsframe) {
			MultiADTSContext mCtx = (MultiADTSContext)ctx;
			for (Entry e : mCtx.list) {
				e.consumer.frame(e.ctx, adtsframe);
			}
		}

		@Override
		public ADTSContext createContext(MediaContext parentContext) {
			return new MultiADTSContext(parentContext, list);
		}
	}

	public class Null implements AdtsFrameConsumer {
		@Override
		public void frame(ADTSContext ctx, ADTSFrame adtsframe) {
		}

		@Override
		public ADTSContext createContext(MediaContext parentContext) {
			return null;
		}
	}

	ADTSContext createContext(MediaContext parentContext);
}
