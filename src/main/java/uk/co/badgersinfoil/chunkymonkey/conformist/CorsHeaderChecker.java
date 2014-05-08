package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public class CorsHeaderChecker implements HttpResponseChecker {

	private static final Set<String> EXPECTED_ALLOW_HEADERS = new HashSet<>();
	static {
		Collections.addAll(EXPECTED_ALLOW_HEADERS,
			"origin", "range", "accept-encoding", "referer"
		);
	}
	private static final Set<String> EXPECTED_EXPOSE_HEADERS = new HashSet<>();
	static {
		Collections.addAll(EXPECTED_EXPOSE_HEADERS,
			"server", "range", "content-length", "content-range"
		);
	}
	private static final Set<String> EXPECTED_ALLOW_METHODS = new HashSet<>();
	static {
		Collections.addAll(EXPECTED_ALLOW_METHODS,
			"get", "head", "options"
		);
	}
	private static final Set<String> EXPECTED_ALLOW_ORIGIN = Collections.singleton("*");

	private Reporter rep;

	public CorsHeaderChecker(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void check(Locator loc, HttpResponse resp, HttpClientContext ctx) {
		List<String> missing = new ArrayList<String>();
		checkHeaderElements(loc, resp, "Access-Control-Allow-Headers", EXPECTED_ALLOW_HEADERS, missing);
		checkHeaderElements(loc, resp, "Access-Control-Expose-Headers", EXPECTED_EXPOSE_HEADERS, missing);
		checkHeaderElements(loc, resp, "Access-Control-Allow-Methods", EXPECTED_ALLOW_METHODS, missing);
		checkHeaderElements(loc, resp, "Access-Control-Allow-Origin", EXPECTED_ALLOW_ORIGIN, missing);
		if (!missing.isEmpty()) {
			rep.carp(loc, "Missing CORS headers: %s", missing);
		}
	}

	private void checkHeaderElements(Locator loc, HttpResponse resp, String headerName, Set<String> expectedElements, List<String> missing) {
		if (resp.containsHeader(headerName)) {
			Set<String> allowHeaders = HeaderUtil.getMergedHeaderElements(resp, headerName);
			if (!expectedElements.equals(allowHeaders)) {
				rep.carp(loc, "%s header values not to spec: ", headerName, allowHeaders);
			}
		} else {
			missing.add(headerName);
		}
	}
}
