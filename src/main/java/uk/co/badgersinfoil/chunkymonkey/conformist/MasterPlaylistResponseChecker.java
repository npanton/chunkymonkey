package uk.co.badgersinfoil.chunkymonkey.conformist;

import org.apache.http.HttpResponse;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public class MasterPlaylistResponseChecker implements HttpResponseChecker {

	private Reporter rep;

	public MasterPlaylistResponseChecker(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void check(Locator loc, HttpResponse resp) {
		if (resp.getStatusLine().getStatusCode() != 200) {
			rep.carp(loc, "Failed %d %s", resp.getStatusLine().getStatusCode(), resp.getStatusLine().getReasonPhrase());
			System.err.println("Failed: "+resp.getStatusLine());
			return;
		}
	}
}
