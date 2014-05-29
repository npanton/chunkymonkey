package uk.co.badgersinfoil.chunkymonkey.conformist;

import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public class MasterPlaylistResponseChecker implements HttpResponseChecker {

	private Reporter rep;

	public MasterPlaylistResponseChecker(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void check(MediaContext mctx, HttpResponse resp, HttpClientContext ctx) {
		if (resp.getStatusLine().getStatusCode() != 200) {
			rep.carp(mctx.getLocator(), "Failed %d %s", resp.getStatusLine().getStatusCode(), resp.getStatusLine().getReasonPhrase());
			System.err.println("Failed: "+resp.getStatusLine());
			return;
		}
	}
}
