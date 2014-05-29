package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.util.Arrays;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
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
	public void check(MediaContext mctx, HttpResponse resp, HttpClientContext ctx) {
		Header[] headers = resp.getHeaders(headerName);
		if (headers.length > 1) {
			rep.carp(mctx.getLocator(), "There should not be multiple '%s' headers: %s", headerName, Arrays.toString(headers));
		} else if (headers.length == 0) {
			rep.carp(mctx.getLocator(), "Response header missing '%s'", headerName);
		} else {
			checkSingleHeaderValue(mctx, headers[0]);
		}
	}

	protected abstract void checkSingleHeaderValue(MediaContext ctx, Header header);
}
