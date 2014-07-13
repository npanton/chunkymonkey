package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.ArrayList;
import java.util.List;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.ts.PIDFilterPacketConsumer.FilterEntry;


public class ProgramTSContext implements MediaContext {

	public class ProgramLocator implements Locator {

		private Locator parent;
		private int pid;

		public ProgramLocator(Locator parent, int pid) {
			this.parent = parent;
			this.pid = pid;
		}

		@Override
		public String toString() {
			return "Program PID="+pid+"\n  at "+parent.toString();
		}

		@Override
		public Locator getParent() {
			return parent;
		}
	}

	private int pid;
	private TransportContext ctx;
	private List<FilterEntry> streams = new ArrayList<>();
	private ProgramMapTable pmt;

	public ProgramTSContext(TransportContext ctx) {
		this.ctx = ctx;
	}

	public TransportContext getTransportContext() {
		return ctx;
	}

	public int getPid() {
		return pid;
	}

	public void addStream(FilterEntry entry) {
		streams.add(entry);
	}

	public boolean removeStream(FilterEntry entry) {
		return streams.remove(entry);
	}

	public void lastPmt(ProgramMapTable pmt) {
		this.pmt = pmt;
	}
	public ProgramMapTable lastPmt() {
		return pmt;
	}

	@Override
	public Locator getLocator() {
		return new ProgramLocator(ctx.getLocator(), pid);
	}
}
