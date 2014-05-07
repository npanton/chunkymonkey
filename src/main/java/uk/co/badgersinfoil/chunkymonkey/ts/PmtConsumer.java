package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface PmtConsumer {
	public static class Multi implements PmtConsumer {
		private List<PmtConsumer> list = new ArrayList<PmtConsumer>();

		public Multi(PmtConsumer... consumers) {
			Collections.addAll(list, consumers);
		}

		@Override
		public void handle(ProgramTSContext progCtx, ProgramMapTable pmt) {
			for (PmtConsumer consumer : list) {
				consumer.handle(progCtx, pmt);
			}
		}
	}
	void handle(ProgramTSContext progCtx, ProgramMapTable pmt);
}
