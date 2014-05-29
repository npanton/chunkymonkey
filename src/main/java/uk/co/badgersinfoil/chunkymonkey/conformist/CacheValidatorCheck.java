package uk.co.badgersinfoil.chunkymonkey.conformist;

import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public class CacheValidatorCheck implements HttpResponseChecker {

	private Reporter rep;

	public CacheValidatorCheck(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void check(MediaContext mctx, HttpResponse resp, HttpClientContext ctx) {
		if (!resp.containsHeader("ETag") && !resp.containsHeader("Last-Modified")) {
			rep.carp(mctx.getLocator(), "Response contains neither ETag nor Last-Modified cache validator headers");
		}
	}

}
