package uk.co.badgersinfoil.chunkymonkey.conformist;

import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Alert;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public class CacheValidatorCheck implements HttpResponseChecker {

	@LogFormat("Response contains neither ETag nor Last-Modified cache validator headers")
	public static final class MissingCacheValidatorEvent extends Alert {
	}

	private Reporter rep;

	public CacheValidatorCheck(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void check(MediaContext mctx, HttpResponse resp, HttpClientContext ctx) {
		if (!resp.containsHeader("ETag") && !resp.containsHeader("Last-Modified")) {
			new MissingCacheValidatorEvent().at(mctx).to(rep);
			// rep.carp(mctx.getLocator(),
			// "Response contains neither ETag nor Last-Modified cache validator headers");
		}
	}

}
