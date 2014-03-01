package uk.co.badgersinfoil.chunkymonkey.adts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface AdtsFrameConsumer {

	void frame(ADTSFrame adtsframe);
	
	public class Multi implements AdtsFrameConsumer {
		List<AdtsFrameConsumer> list = new ArrayList<>();
		
		public Multi(AdtsFrameConsumer... list) {
			this.list = Arrays.asList(list);
		}
		public Multi(List<AdtsFrameConsumer> list) {
			this.list = list;
		}

		@Override
		public void frame(ADTSFrame adtsframe) {
			for (AdtsFrameConsumer consumer : list) {
				consumer.frame(adtsframe);
			}
		}
	}

	public class Null implements AdtsFrameConsumer {
		@Override
		public void frame(ADTSFrame adtsframe) {
		}
	}
}
