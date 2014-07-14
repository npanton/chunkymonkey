package uk.co.badgersinfoil.chunkymonkey.hls;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.execchain.RequestAbortedException;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Alert;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;

/**
 * Wraps {@link HttpClient#execute(HttpUriRequest, HttpContext)} in order to
 * capture and report on diagnostic information for (network) failures.
 */
public abstract class HttpExecutionWrapper<T> {
	@LogFormat("{remoteAddress}: {message}, after {durationMillis}ms")
	public static class ConnectTimeoutEvent extends Alert {}
	@LogFormat("{message} connecting to {remoteAddress}")
	public static class ConnectFailedEvent extends Alert {}
	@LogFormat("Failed to obtain a connection from pool after {durationMillis}ms")
	public static class ConnectPoolTimeoutEvent extends Alert {}
	@LogFormat("Request failed {statusCode} {reasonPhrase} - headers: {responseHeaders}")
	public static class RequestFailedEvent extends Alert {}
	@LogFormat("HTTP request failed after {durationMillis}ms: {message}")
	public static class SocketTimeoutEvent extends Alert {}
	@LogFormat("HTTP request failed after {durationMillis}ms (initial {statusLine} response): {message}")
	public static class SocketTimeoutAfterHeadersEvent extends Alert {}
	@LogFormat("{message}, after {durationMillis}ms")
	public static class ConnectionClosedEvent extends Alert {}
	@LogFormat("HTTP request failed: {message}")
	public static class HttpFailedEvent extends Alert {}
	@LogFormat("Unknown host {unknownHost}")
	public static class UnknownHostEvent extends Alert {}

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

	/**
	 * When httpclient fails to connect to an IP address, this object allows
	 * us to capture and record information about the failure, which might
	 * otherwise be obscured if httpclient is able to retry connecting to
	 * other IP addresses associated with the given hostname.
	 */
	public static final ConnectionSocketFactory CONNECT_FAILURE_CLASSIFYING_SOCKET_FACTORY
		= new ConnectionSocketFactory() {
			private ConnectionSocketFactory delegate = PlainConnectionSocketFactory.INSTANCE;

			@Override
			public Socket createSocket(HttpContext context)
				throws IOException
			{
				return delegate.createSocket(context);
			}

			@Override
			public Socket connectSocket(int connectTimeout,
			                            Socket sock,
			                            HttpHost host,
			                            InetSocketAddress remoteAddress,
			                            InetSocketAddress localAddress,
			                            HttpContext context)
				throws IOException
			{
				HttpStat stat = (HttpStat)context.getAttribute("chunkymonkey-http-stat");
				Reporter rep = (Reporter)context.getAttribute("chunkymonkey-reporter");
				MediaContext ctx = (MediaContext)context.getAttribute("chunkymonkey-context");
				try {
					return delegate.connectSocket(connectTimeout, sock, host, remoteAddress, localAddress, context);
				} catch (ConnectException e) {
					report(e, stat, rep, remoteAddress, ctx);
					throw e;
				} catch (SocketTimeoutException e) {
					report(e, stat, rep, remoteAddress, ctx);
					throw e;
				} catch (IOException e) {
					System.err.println("Problem connecting to "+remoteAddress);
					e.printStackTrace();
					throw e;
				}
			}

			private void report(ConnectException e,
			                    HttpStat stat,
			                    Reporter rep,
			                    InetSocketAddress remoteAddress,
			                    MediaContext ctx)
			{
				stat.sock(remoteAddress.getAddress());
				stat.connectFailed();
				new ConnectFailedEvent()
					.with("message", e.getMessage())
					.with("remoteAddress", remoteAddress.getAddress())
					.at(ctx)
					.to(rep);
			}

			private void report(SocketTimeoutException e,
			                    HttpStat stat,
			                    Reporter rep,
			                    InetSocketAddress remoteAddress,
			                    MediaContext ctx)
			{
				stat.sock(remoteAddress.getAddress());
				stat.connectTimeout();
				new ConnectTimeoutEvent()
					.with("message", e.getMessage())
					.with("remoteAddress", remoteAddress.getAddress())
					.with("durationMillis", stat.getDurationMillis())
					.at(ctx)
					.to(rep);
			}
		};


	public T execute(HttpClient httpclient, HttpUriRequest req, MediaContext ctx, HttpStat stat) {
		HttpClientContext context = HttpClientContext.create();
		context.setAttribute("chunkymonkey-http-stat", stat);
		context.setAttribute("chunkymonkey-reporter", rep);
		context.setAttribute("chunkymonkey-context", ctx);
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
		} catch (ConnectionPoolTimeoutException e) {
			stat.failed();
			new ConnectPoolTimeoutEvent()
				.with("durationMillis", stat.getDurationMillis())
				.at(ctx)
				.to(rep);
		} catch (ConnectTimeoutException e) {
			stat.failed();
			// already handled by CONNECT_FAILURE_CLASSIFYING_SOCKET_FACTORY
		} catch (HttpHostConnectException e) {
			stat.failed();
			// already handled by CONNECT_FAILURE_CLASSIFYING_SOCKET_FACTORY
		} catch (RequestAbortedException e) {
			// our ScheduledExecutorService is presumably being
			// shut down, so ignore this.
		} catch (UnknownHostException e) {
			stat.failed();
			new UnknownHostEvent()
				.with("unknownHost", e.getMessage())
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
				.with("message", e.toString())
				.with("durationMillis", stat.getDurationMillis())
				.at(ctx)
				.to(rep);
		} finally {
			if (resp != null) {
				try {
					resp.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}

	private static InetAddress getRemote(HttpClientContext context) {
		return (InetAddress)context.getAttribute("conformist-remote-address");
	}

	protected abstract T handleResponse(HttpClientContext context, CloseableHttpResponse resp, HttpStat stat) throws IOException;
}
