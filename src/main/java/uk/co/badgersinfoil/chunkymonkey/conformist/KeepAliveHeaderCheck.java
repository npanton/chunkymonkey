package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.util.Arrays;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Alert;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public class KeepAliveHeaderCheck implements HttpResponseChecker {

	@LogFormat("Connection header should specify the value 'keep-alive': {keepAliveValue}")
	public static final class MissingKeepAliveValueEvent extends Alert {
	}

	@LogFormat("Connection header should be present, and specify the value 'keep-alive'")
	public static final class MissingKeepAliveEvent extends Alert {
	}

	private static final String CONNECTION = "Connection";
	private Reporter rep;

	public KeepAliveHeaderCheck(Reporter rep) {
		this.rep = rep;
	}

	public void check(MediaContext mctx, HttpResponse resp) {
		// at least the first response should allow keep-alive, but
		// let the server close the connection later if it wants to
		if (resp.containsHeader(CONNECTION)) {
			Set<String> connection = HeaderUtil.getMergedHeaderElements(resp, CONNECTION);
			if (!connection.contains("keep-alive")) {
				// rep.carp(mctx.getLocator(),
				// "Connection header should specify the value 'keep-alive': %s",
				// Arrays.toString(resp.getHeaders(CONNECTION)));
				new MissingKeepAliveValueEvent().with("keepAliveValue", Arrays.toString(resp.getHeaders(CONNECTION))).at(mctx).to(rep);
			}
		} else {
			// rep.carp(mctx.getLocator(),
			// "Connection header should be present, and specify the value 'keep-alive'");
			new MissingKeepAliveEvent().at(mctx).to(rep);
		}
	}

	@Override
	public void check(MediaContext mctx, HttpResponse resp, HttpClientContext ctx) {
		// at least the first response should allow keep-alive, but
		// let the server close the connection later if it wants to
		if (ctx.getConnection().getMetrics().getRequestCount() == 1) {
			if (resp.containsHeader(CONNECTION)) {
				Set<String> connection = HeaderUtil.getMergedHeaderElements(resp, CONNECTION);
				if (!connection.contains("keep-alive")) {
					// rep.carp(mctx.getLocator(),
					// "Connection header should specify the value 'keep-alive': %s",
					// Arrays.toString(resp.getHeaders(CONNECTION)));
					new MissingKeepAliveValueEvent().with("keepAliveValue", Arrays.toString(resp.getHeaders(CONNECTION))).at(mctx).to(rep);
				}
			} else {
				// rep.carp(mctx.getLocator(),
				// "Connection header should be present, and specify the value 'keep-alive'");
				new MissingKeepAliveEvent().at(mctx).to(rep);
			}
		}
	}
}
