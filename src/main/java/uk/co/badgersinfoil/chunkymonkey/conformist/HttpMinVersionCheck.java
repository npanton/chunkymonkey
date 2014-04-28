package uk.co.badgersinfoil.chunkymonkey.conformist;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.protocol.HttpClientContext;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public class HttpMinVersionCheck implements HttpResponseChecker {
	private ProtocolVersion version;
	protected Reporter rep;

	public HttpMinVersionCheck(ProtocolVersion version, Reporter rep) {
		this.version = version;
		this.rep = rep;
	}

	@Override
	public void check(Locator loc, HttpResponse resp, HttpClientContext ctx) {
		if (!resp.getStatusLine().getProtocolVersion().greaterEquals(version)) {
			rep.carp(loc, "Minimum protocol version is %s: got %s", version, resp.getStatusLine().getProtocolVersion());
		}
	}
}
