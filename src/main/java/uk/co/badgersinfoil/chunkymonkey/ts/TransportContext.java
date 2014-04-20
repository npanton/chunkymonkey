package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.HashMap;
import java.util.Map;
import uk.co.badgersinfoil.chunkymonkey.ts.PIDFilterPacketConsumer.FilterEntry;


public class TransportContext implements TSContext, TransportContextProvider {
	private Map<Integer, FilterEntry> filterMap = new HashMap<>();

	public TransportContext() {
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

}
