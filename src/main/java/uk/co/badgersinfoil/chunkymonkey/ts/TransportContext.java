package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.co.badgersinfoil.chunkymonkey.ts.PIDFilterPacketConsumer.FilterEntry;


public class TransportContext implements TSContext, TransportContextProvider {
	private Map<Integer, FilterEntry> filterMap = new HashMap<>();
	private TSContext parent;

	public TransportContext(TSContext parent) {
		this.parent = parent;
	}

	public FilterEntry filterForPid(int pid) {
		return filterMap.get(pid);
	}

	public void filter(Integer key, FilterEntry filterEntry) {
		filterMap.put(key, filterEntry);
	}

	@Override
	public TransportContext getTransportContext() {
		return this;
	}

	public List<TSContext> getContexts() {
		List<TSContext> result = new ArrayList<TSContext>();
		for (FilterEntry e : filterMap.values()) {
			result.add(e.getContext());
		}
		return result;
	}

	public TSContext getParent() {
		return parent;
	}
}
