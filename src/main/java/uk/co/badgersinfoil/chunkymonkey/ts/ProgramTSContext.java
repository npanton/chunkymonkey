package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.ArrayList;
import java.util.List;

import uk.co.badgersinfoil.chunkymonkey.ts.PIDFilterPacketConsumer.FilterEntry;


public class ProgramTSContext implements TSContext {

	private int pid;
	private TransportContext ctx;
	private List<FilterEntry> streams = new ArrayList<>();
	private ProgramMapTable pmt;

	public ProgramTSContext(TransportContext ctx) {
		this.ctx = ctx;
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
}
