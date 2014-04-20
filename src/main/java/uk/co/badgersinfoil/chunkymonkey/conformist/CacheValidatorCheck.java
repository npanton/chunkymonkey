package uk.co.badgersinfoil.chunkymonkey.conformist;

import org.apache.http.HttpResponse;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public class CacheValidatorCheck implements HttpResponseChecker {

	private Reporter rep;

	public CacheValidatorCheck(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void check(Locator loc, HttpResponse resp) {
		if (!resp.containsHeader("ETag") && !resp.containsHeader("Last-Modified")) {
			rep.carp(loc, "Response contains neither ETag nor Last-Modified cache validator headers");
		}
	}

}
