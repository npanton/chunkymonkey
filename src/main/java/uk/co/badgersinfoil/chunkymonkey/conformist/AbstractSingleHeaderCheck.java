package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.util.Arrays;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public abstract class AbstractSingleHeaderCheck implements HttpResponseChecker {

	private String headerName;
	protected Reporter rep;

	public AbstractSingleHeaderCheck(String headerName, Reporter rep) {
		this.headerName = headerName;
		this.rep = rep;
	}

	@Override
	public void check(Locator loc, HttpResponse resp, HttpClientContext ctx) {
		Header[] headers = resp.getHeaders(headerName);
		if (headers.length > 1) {
			rep.carp(loc, "There should not be multiple '%s' headers: %s", headerName, Arrays.toString(headers));
		} else if (headers.length == 0) {
			rep.carp(loc, "Response header missing '%s'", headerName);
		} else {
			checkSingleHeaderValue(loc, headers[0]);
		}
	}

	protected abstract void checkSingleHeaderValue(Locator loc, Header header);
}
