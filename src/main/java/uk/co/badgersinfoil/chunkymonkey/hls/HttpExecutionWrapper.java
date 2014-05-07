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
import uk.co.badgersinfoil.chunkymonkey.Reporter;

/**
 * Wraps {@link HttpClient#execute(HttpUriRequest, HttpContext)} in order to
 * capture and report on diagnostic information for (network) failures.
 */
public abstract class HttpExecutionWrapper<T> {

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

	public T execute(HttpClient httpclient, HttpUriRequest req, Locator loc, HttpStat stat) {
		HttpClientContext context = HttpClientContext.create();
		CloseableHttpResponse resp = null;
		stat.start();
		try {
			resp = (CloseableHttpResponse)httpclient.execute(req, context);
			stat.headers(resp.getStatusLine().getStatusCode());
			// TODO: pull X-Pkgr-Instance handling out into optional component
			if (resp.containsHeader("X-Pkgr-Instance")) {
				loc = new PackagerInstanceLocator(resp.getLastHeader("X-Pkgr-Instance").getValue(), loc);
			}
			// TODO: not enough to work when we need to handle
			// conditional HTTP (not-modified resposes) etc.
			T result = null;
			if (resp.getStatusLine().getStatusCode() == 200) {
				result = handleResponse(context, resp, stat);
			} else {
				rep.carp(loc, "Request failed %d %s - headers: %s", resp.getStatusLine().getStatusCode(), resp.getStatusLine().getReasonPhrase(), Arrays.toString(resp.getAllHeaders()));
			}
			stat.end();
			resp.close();
			return result;
		} catch (SocketTimeoutException e) {
			InetAddress remote = getRemote(context);
			stat.sock(remote);
			stat.timeout();
			if (remote != null) {
				loc = new SockLocator(remote, loc);
			}
			// e.bytesTransferred always seems to be 0, so we don't
			// bother reporting it
			if (resp == null || resp.getStatusLine() != null) {
				rep.carp(loc, "HTTP request failed after %dms: %s", stat.getDurationMillis(), e.getMessage());
			} else {
				rep.carp(loc, "HTTP request failed after %dms, initial %s response: %s", stat.getDurationMillis(), resp.getStatusLine(), e.getMessage());
			}
		} catch (ConnectionClosedException e) {
			InetAddress remote = getRemote(context);
			if (remote != null) {
				loc = new SockLocator(remote, loc);
				stat.sock(remote);
			}
			stat.prematureClose();
			if (resp == null || resp.getFirstHeader("Connection") == null) {
				rep.carp(loc, "%s, after %dms", e.getMessage(), stat.getDurationMillis());
			} else {
				rep.carp(loc, "%s, after %dms (response included %s)", e.getMessage(), stat.getDurationMillis(), resp.getFirstHeader("Connection"));
			}
		} catch (ConnectTimeoutException e) {
			InetAddress remote = getRemote(context);
			if (remote != null) {
				loc = new SockLocator(remote, loc);
				stat.sock(remote);
			}
			stat.connectTimeout();
			rep.carp(loc, "%s, after %dms", e.getMessage(), stat.getDurationMillis());
		} catch (IOException e) {
			InetAddress remote = getRemote(context);
			if (remote != null) {
				loc = new SockLocator(remote, loc);
				stat.sock(remote);
			}
			stat.failed();
			// TODO: parent Locator
			rep.carp(loc, "HTTP request failed: %s", e.toString());
		}
		return null;
	}

	private static InetAddress getRemote(HttpClientContext context) {
		return (InetAddress)context.getAttribute("conformist-remote-address");
	}

	protected abstract T handleResponse(HttpClientContext context, CloseableHttpResponse resp, HttpStat stat) throws IOException;
}
