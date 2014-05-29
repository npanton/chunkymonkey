package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.util.Arrays;
import java.util.Set;
import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public class KeepAliveHeaderCheck implements HttpResponseChecker {

	private static final String CONNECTION = "Connection";
	private Reporter rep;

	public KeepAliveHeaderCheck(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void check(MediaContext mctx, HttpResponse resp, HttpClientContext ctx) {
		// at least the first response should allow keep-alive, but
		// let the server close the connection later if it wants to
		if (ctx.getConnection().getMetrics().getRequestCount() == 1) {
			if (resp.containsHeader(CONNECTION)) {
				Set<String> connection = HeaderUtil.getMergedHeaderElements(resp, CONNECTION);
				if (!connection.contains("keep-alive")) {
					rep.carp(mctx.getLocator(), "Connection header should specify the value 'keep-alive': %s", Arrays.toString(resp.getHeaders(CONNECTION)));
				}
			} else {
				rep.carp(mctx.getLocator(), "Connection header should be present, and specify the value 'keep-alive'");
			}
		}
	}
}
