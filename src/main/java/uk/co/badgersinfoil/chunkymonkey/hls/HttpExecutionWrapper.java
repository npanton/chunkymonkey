package uk.co.badgersinfoil.chunkymonkey.hls;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.Reporter.Event;
import uk.co.badgersinfoil.chunkymonkey.Reporter.LogFormat;

/**
 * Wraps {@link HttpClient#execute(HttpUriRequest, HttpContext)} in order to
 * capture and report on diagnostic information for (network) failures.
 */
public abstract class HttpExecutionWrapper<T> {
	@LogFormat("{message}, after {durationMillis}ms")
	public static class ConnectTimeoutEvent extends Event {}
	@LogFormat("Request failed {statusCode} {reasonPhrase} - headers: {responseHeaders}")
	public static class RequestFailedEvent extends Event {}
	@LogFormat("HTTP request failed after {durationMillis}ms: {message}")
	public static class SocketTimeoutEvent extends Event {}
	@LogFormat("HTTP request failed after {durationMillis}ms (initial {statusLine} response): {message}")
	public static class SocketTimeoutAfterHeadersEvent extends Event {}
	@LogFormat("{message}, after {durationMillis}ms")
	public static class ConnectionClosedEvent extends Event {}
	@LogFormat("HTTP request failed: {message}")
	public static class HttpFailedEvent extends Event {}

	public class SockContext implements MediaContext {

		private InetAddress remote;
		private MediaContext parentContext;

		public SockContext(InetAddress remote, MediaContext parentContext) {
			this.remote = remote;
			this.parentContext = parentContext;
		}

		@Override
		public Locator getLocator() {
			return new SockLocator(remote, parentContext.getLocator());
		}

	}

	public class PackagerInstanceContext implements MediaContext {

		private String instanceId;
		private MediaContext parentContext;

		public PackagerInstanceContext(String instanceId, MediaContext parentContext) {
			this.instanceId = instanceId;
			this.parentContext = parentContext;
		}

		@Override
		public Locator getLocator() {
			return new PackagerInstanceLocator(instanceId, parentContext.getLocator());
		}

	}

	private Reporter rep;

	public HttpExecutionWrapper(Reporter rep) {
		this.rep = rep;
	}

	public static final HttpRequestExecutor CONN_INFO_SNARFING_REQUEST_EXECUTOR
		= new HttpRequestExecutor() {
			@Override
			protected HttpResponse doSendRequest(
					HttpRequest request,
					HttpClientConnection conn,
					HttpContext context)
					throws IOException, HttpException {
				HttpInetConnection iConn = (HttpInetConnection)conn;
				context.setAttribute("conformist-remote-address", iConn.getRemoteAddress());
				return super.doSendRequest(request, conn, context);
			}
		};

	public T execute(HttpClient httpclient, HttpUriRequest req, MediaContext ctx, HttpStat stat) {
		HttpClientContext context = HttpClientContext.create();
		CloseableHttpResponse resp = null;
		stat.start();
		try {
			resp = (CloseableHttpResponse)httpclient.execute(req, context);
			stat.headers(resp.getStatusLine().getStatusCode());
			// TODO: pull X-Pkgr-Instance handling out into optional component
			if (resp.containsHeader("X-Pkgr-Instance")) {
				ctx = new PackagerInstanceContext(resp.getLastHeader("X-Pkgr-Instance").getValue(), ctx);
			}
			T result = null;
			int statusCode = resp.getStatusLine().getStatusCode();
			if (statusCode == 200 || statusCode == 304) {
				result = handleResponse(context, resp, stat);
			} else {
				new RequestFailedEvent()
					.with("statusCode", resp.getStatusLine().getStatusCode())
					.with("reasonPhrase", resp.getStatusLine().getReasonPhrase())
					.with("responseHeaders", Arrays.toString(resp.getAllHeaders()))
					.at(ctx)
					.to(rep);
			}
			stat.end();
			resp.close();
			return result;
		} catch (SocketTimeoutException e) {
			InetAddress remote = getRemote(context);
			stat.sock(remote);
			stat.timeout();
			if (remote != null) {
				ctx = new SockContext(remote, ctx);
			}
			// e.bytesTransferred always seems to be 0, so we don't
			// bother reporting it
			if (resp == null || resp.getStatusLine() != null) {
				new SocketTimeoutEvent()
					.with("durationMillis", stat.getDurationMillis())
					.with("message", e.getMessage())
					.at(ctx)
					.to(rep);
			} else {
				new SocketTimeoutAfterHeadersEvent()
					.with("durationMillis", stat.getDurationMillis())
					.with("statusLine", resp.getStatusLine())
					.with("message", e.getMessage())
					.at(ctx)
					.to(rep);
			}
		} catch (ConnectionClosedException e) {
			InetAddress remote = getRemote(context);
			if (remote != null) {
				ctx = new SockContext(remote, ctx);
				stat.sock(remote);
			}
			stat.prematureClose();
			new ConnectionClosedEvent()
				.with("message", e.getMessage())
				.with("durationMillis", stat.getDurationMillis())
				.at(ctx)
				.to(rep);
		} catch (ConnectTimeoutException e) {
			InetAddress remote = getRemote(context);
			if (remote != null) {
				ctx = new SockContext(remote, ctx);
				stat.sock(remote);
			}
			stat.connectTimeout();
			new ConnectTimeoutEvent()
				.with("message", e.getMessage())
				.with("durationMillis", stat.getDurationMillis())
				.at(ctx)
				.to(rep);
		} catch (IOException e) {
			InetAddress remote = getRemote(context);
			if (remote != null) {
				ctx = new SockContext(remote, ctx);
				stat.sock(remote);
			}
			stat.failed();
			new HttpFailedEvent()
				.with("message", e.getMessage())
				.with("durationMillis", stat.getDurationMillis())
				.at(ctx)
				.to(rep);
		}
		return null;
	}

	private static InetAddress getRemote(HttpClientContext context) {
		return (InetAddress)context.getAttribute("conformist-remote-address");
	}

	protected abstract T handleResponse(HttpClientContext context, CloseableHttpResponse resp, HttpStat stat) throws IOException;
}
