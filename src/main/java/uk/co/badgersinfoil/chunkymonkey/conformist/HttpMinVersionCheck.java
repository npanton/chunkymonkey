package uk.co.badgersinfoil.chunkymonkey.conformist;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.protocol.HttpClientContext;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Alert;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public class HttpMinVersionCheck implements HttpResponseChecker {
	private ProtocolVersion version;
	protected Reporter rep;

	@LogFormat("Minimum protocol version is {minVersion}: got {derrivedVersion}")
	public static final class MissingProtocolVersionEvent extends Alert {
	}

	public HttpMinVersionCheck(ProtocolVersion version, Reporter rep) {
		this.version = version;
		this.rep = rep;
	}

	@Override
	public void check(MediaContext mctx, HttpResponse resp, HttpClientContext ctx) {
		if (!resp.getStatusLine().getProtocolVersion().greaterEquals(version)) {
			new MissingProtocolVersionEvent().with("minVersion", version).with("derrivedVersion", resp.getStatusLine().getProtocolVersion()).at(mctx).to(rep);
			// rep.carp(mctx.getLocator(),
			// "Minimum protocol version is %s: got %s", version,
			// resp.getStatusLine().getProtocolVersion());
		}
	}
}
