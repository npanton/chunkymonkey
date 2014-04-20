package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.util.Arrays;
import java.util.Set;
import org.apache.http.HttpResponse;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public class KeepAliveHeaderCheck implements HttpResponseChecker {

	private static final String CONNECTION = "Connection";
	private Reporter rep;

	public KeepAliveHeaderCheck(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void check(Locator loc, HttpResponse resp) {
		if (resp.containsHeader(CONNECTION)) {
			Set<String> connection = HeaderUtil.getMergedHeaderElements(resp, CONNECTION);
			if (!connection.contains("keep-alive")) {
				rep.carp(loc, "Connection header should specify the value 'keep-alive': %s", Arrays.toString(resp.getHeaders(CONNECTION)));
			}
		} else {
			rep.carp(loc, "Connection header should be present, and specify the value 'keep-alive'");
		}
	}
}
