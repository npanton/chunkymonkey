package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.ts.PIDFilterPacketConsumer.FilterEntry;


public class TransportContext implements MediaContext, TransportContextProvider {
	private Map<Integer, FilterEntry> filterMap = new HashMap<>();
	private MediaContext parent;

	public TransportContext(MediaContext parent) {
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

	public List<MediaContext> getContexts() {
		List<MediaContext> result = new ArrayList<MediaContext>();
		for (FilterEntry e : filterMap.values()) {
			result.add(e.getContext());
		}
		return result;
	}

	public MediaContext getParent() {
		return parent;
	}

	@Override
	public Locator getLocator() {
		return parent.getLocator();
	}
}
