package uk.co.badgersinfoil.chunkymonkey.hls;

import java.net.InetAddress;

public class HttpStat {

	public enum EndState {
		TIMEOUT, COMPLETED, PREMATURE_CLOSE

	}

	private long startTime;
	private Integer statusCode = null;
	private Long headersTime = null;
	private Long end = null;
	private InetAddress remote;
	private EndState endState;

	private HttpStat() {
	}

	public static HttpStat start() {
		HttpStat s = new HttpStat();
		s.startTime = System.currentTimeMillis();
		return s;
	}

	public void headers(int statusCode) {
		this.statusCode  = statusCode;
		headersTime = System.currentTimeMillis();
	}

	public void end() {
		end = System.currentTimeMillis();
		endState = EndState.COMPLETED;
	}

	public void sock(InetAddress remote) {
		this.remote = remote;
	}

	public void timeout() {
		end = System.currentTimeMillis();
		endState = EndState.TIMEOUT;
	}

	public void prematureClose() {
		end = System.currentTimeMillis();
		endState = EndState.PREMATURE_CLOSE;
	}

	public long getDurationMillis() {
		if (end == null) {
			throw new IllegalStateException("HTTP response not yet ended");
		}
		return end - startTime;
	}
}
