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

/**
 * Wraps {@link HttpClient#execute(HttpUriRequest, HttpContext)} in order to
 * capture and report on diagnostic information for (network) failures.
 */
public abstract class HttpExecutionWrapper<T> {

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
				rep.carp(ctx.getLocator(), "Request failed %d %s - headers: %s", resp.getStatusLine().getStatusCode(), resp.getStatusLine().getReasonPhrase(), Arrays.toString(resp.getAllHeaders()));
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
				rep.carp(ctx.getLocator(), "HTTP request failed after %dms: %s", stat.getDurationMillis(), e.getMessage());
			} else {
				rep.carp(ctx.getLocator(), "HTTP request failed after %dms, initial %s response: %s", stat.getDurationMillis(), resp.getStatusLine(), e.getMessage());
			}
		} catch (ConnectionClosedException e) {
			InetAddress remote = getRemote(context);
			if (remote != null) {
				ctx = new SockContext(remote, ctx);
				stat.sock(remote);
			}
			stat.prematureClose();
			if (resp == null || resp.getFirstHeader("Connection") == null) {
				rep.carp(ctx.getLocator(), "%s, after %dms", e.getMessage(), stat.getDurationMillis());
			} else {
				rep.carp(ctx.getLocator(), "%s, after %dms (response included %s)", e.getMessage(), stat.getDurationMillis(), resp.getFirstHeader("Connection"));
			}
		} catch (ConnectTimeoutException e) {
			InetAddress remote = getRemote(context);
			if (remote != null) {
				ctx = new SockContext(remote, ctx);
				stat.sock(remote);
			}
			stat.connectTimeout();
			rep.carp(ctx.getLocator(), "%s, after %dms", e.getMessage(), stat.getDurationMillis());
		} catch (IOException e) {
			InetAddress remote = getRemote(context);
			if (remote != null) {
				ctx = new SockContext(remote, ctx);
				stat.sock(remote);
			}
			stat.failed();
			// TODO: parent Locator
			rep.carp(ctx.getLocator(), "HTTP request failed: %s", e.toString());
		}
		return null;
	}

	private static InetAddress getRemote(HttpClientContext context) {
		return (InetAddress)context.getAttribute("conformist-remote-address");
	}

	protected abstract T handleResponse(HttpClientContext context, CloseableHttpResponse resp, HttpStat stat) throws IOException;
}
