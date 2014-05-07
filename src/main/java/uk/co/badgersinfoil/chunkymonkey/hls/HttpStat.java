package uk.co.badgersinfoil.chunkymonkey.hls;

import java.net.InetAddress;

public class HttpStat {

	public enum EndState {
		/**
		 * Timeout due to data transfer going idle
		 */
		TIMEOUT,
		/**
		 * Timemout while trying to connect
		 */
		CONNECT_TIMEOUT,
		/**
		 * Completed without network failure (may still have had HTTP
		 * error status)
		 */
		COMPLETED,
		/**
		 * Connection closed before whole body received (may still have
		 * had HTTP success status)
		 */
		PREMATURE_CLOSE,
		/**
		 * Unspecified network failure
		 */
		FAILED
	}

	private long startTime;
	private Integer statusCode = null;
	private Long headersTime = null;
	private Long end = null;
	private InetAddress remote;
	private EndState endState;

	public HttpStat() {
	}

	public void start() {
		startTime = System.currentTimeMillis();
	}

	public void headers(int statusCode) {
		this.statusCode  = statusCode;
		headersTime = System.currentTimeMillis();
	}

	public void end() {
		if (end == null) {
			end = System.currentTimeMillis();
			endState = EndState.COMPLETED;
		}
	}

	public void sock(InetAddress remote) {
		this.remote = remote;
	}

	public void timeout() {
		end = System.currentTimeMillis();
		endState = EndState.TIMEOUT;
	}

	public void connectTimeout() {
		end = System.currentTimeMillis();
		endState = EndState.CONNECT_TIMEOUT;
	}

	public void prematureClose() {
		end = System.currentTimeMillis();
		endState = EndState.PREMATURE_CLOSE;
	}

	public void failed() {
		end = System.currentTimeMillis();
		endState = EndState.FAILED;
	}

	public long getDurationMillis() {
		if (end == null) {
			throw new IllegalStateException("HTTP response not yet ended");
		}
		return end - startTime;
	}

	public Integer getStatusCode() {
		return statusCode;
	}

	public EndState getEndState() {
		return endState;
	}

	public InetAddress getRemote() {
		return remote;
	}
}
