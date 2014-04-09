package uk.co.badgersinfoil.chunkymonkey.adts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.co.badgersinfoil.chunkymonkey.MediaDuration;

public interface AdtsFrameConsumer {

	void frame(ADTSContext ctx, ADTSFrame adtsframe);
	
	public class Multi implements AdtsFrameConsumer {
		private static class MultiADTSContext implements ADTSContext {
			private List<Entry> list = new ArrayList<>();
			public MultiADTSContext(List<AdtsFrameConsumer> list) {
				for (AdtsFrameConsumer p : list) {
					Entry e = new Entry();
					e.consumer = p;
					e.ctx = p.createContext();
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
		public ADTSContext createContext() {
			return new MultiADTSContext(list);
		}
	}

	public class Null implements AdtsFrameConsumer {
		@Override
		public void frame(ADTSContext ctx, ADTSFrame adtsframe) {
		}

		@Override
		public ADTSContext createContext() {
			return null;
		}
	}

	ADTSContext createContext();
}
