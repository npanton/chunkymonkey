package uk.co.badgersinfoil.chunkymonkey.hls;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpStat.EndState;

public class HttpStats {

	private AtomicLong requests = new AtomicLong();
	private Map<Integer,AtomicLong> statuses = new HashMap<>();
	private Map<InetAddress,AtomicLong> prematureClosesByRemoteIp = new HashMap<>();
	private Map<InetAddress,AtomicLong> timeoutsByRemoteIp = new HashMap<>();
	private Map<InetAddress,AtomicLong> connectTimeoutsByRemoteIp = new HashMap<>();
	private Map<HttpStat.EndState,AtomicLong> endStates = new HashMap<>();
	private AtomicLong unspecifiedFailures = new AtomicLong();
	private AtomicLong totalTime = new AtomicLong();

	public HttpStats() {
		for (EndState s : HttpStat.EndState.values()) {
			endStates.put(s, new AtomicLong());
		}
	}

	public void add(HttpStat stat) {
		requests.incrementAndGet();
		if (stat.getStatusCode() != null) {
			status(stat.getStatusCode());
		}
		endStates.get(stat.getEndState()).incrementAndGet();
		totalTime.addAndGet(stat.getDurationMillis());
		switch (stat.getEndState()) {
		case PREMATURE_CLOSE:
			prematureCloseByRemoteIp(stat.getRemote());
			break;
		case TIMEOUT:
			timeoutByRemoteIp(stat.getRemote());
			break;
		case CONNECT_TIMEOUT:
			connectTimeoutByRemoteIp(stat.getRemote());
			break;
		case FAILED:
			unspecifiedFailures.incrementAndGet();
		case COMPLETED:
			// nothing more to do
		}
	}

	private void connectTimeoutByRemoteIp(InetAddress remote) {
		AtomicLong count;
		synchronized (connectTimeoutsByRemoteIp) {
			count = connectTimeoutsByRemoteIp.get(remote);
			if (count == null) {
				count = new AtomicLong();
				connectTimeoutsByRemoteIp.put(remote, count);
			}
		}
		count.incrementAndGet();
	}

	private void timeoutByRemoteIp(InetAddress remote) {
		AtomicLong count;
		synchronized (timeoutsByRemoteIp) {
			count = timeoutsByRemoteIp.get(remote);
			if (count == null) {
				count = new AtomicLong();
				timeoutsByRemoteIp.put(remote, count);
			}
		}
		count.incrementAndGet();
	}

	private void prematureCloseByRemoteIp(InetAddress remote) {
		AtomicLong count;
		synchronized (prematureClosesByRemoteIp) {
			count = prematureClosesByRemoteIp.get(remote);
			if (count == null) {
				count = new AtomicLong();
				prematureClosesByRemoteIp.put(remote, count);
			}
		}
		count.incrementAndGet();
	}

	public long getRequests() {
		return requests.get();
	}
	public long getUnspecifiedFailures() {
		return unspecifiedFailures.get();
	}

	private void status(int statusCode) {
		AtomicLong count;
		synchronized (statuses) {
			count = statuses.get(statusCode);
			if (count == null) {
				count = new AtomicLong();
				statuses.put(statusCode, count);
			}
		}
		count.incrementAndGet();
	}

	public Map<Integer,Long> getStatusCounts() {
		Map<Integer,Long> result = new HashMap<>();
		synchronized (statuses) {
			for (Entry<Integer, AtomicLong> e : statuses.entrySet()) {
				result.put(e.getKey(), e.getValue().get());
			}
		}
		return result;
	}

	public Map<InetAddress,Long> getPrematureClosesByRemoteIp() {
		Map<InetAddress,Long> result = new HashMap<>();
		synchronized (prematureClosesByRemoteIp) {
			for (Entry<InetAddress, AtomicLong> e : prematureClosesByRemoteIp.entrySet()) {
				result.put(e.getKey(), e.getValue().get());
			}
		}
		return result;
	}

	public Map<InetAddress,Long> getTimeoutsByRemoteIp() {
		Map<InetAddress,Long> result = new HashMap<>();
		synchronized (timeoutsByRemoteIp) {
			for (Entry<InetAddress, AtomicLong> e : timeoutsByRemoteIp.entrySet()) {
				result.put(e.getKey(), e.getValue().get());
			}
		}
		return result;
	}

	public Map<InetAddress,Long> getConnectTimeoutsByRemoteIp() {
		Map<InetAddress,Long> result = new HashMap<>();
		synchronized (connectTimeoutsByRemoteIp) {
			for (Entry<InetAddress, AtomicLong> e : connectTimeoutsByRemoteIp.entrySet()) {
				result.put(e.getKey(), e.getValue().get());
			}
		}
		return result;
	}
}
