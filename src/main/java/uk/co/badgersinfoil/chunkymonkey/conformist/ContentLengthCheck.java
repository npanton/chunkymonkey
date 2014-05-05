package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.util.Arrays;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public class ContentLengthCheck implements HttpResponseChecker {

	private Reporter rep;

	public ContentLengthCheck(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void check(Locator loc, HttpResponse resp, HttpClientContext ctx) {
		Header header = (Header)ctx.getAttribute(ContentLengthSnarfer.ORIGINAL_CONTENT_LENGTH);
		if (header == null) {
			rep.carp(loc, "Response lacks 'Content-Length' header: %s", Arrays.toString(resp.getAllHeaders()));
			return;
		}
		long contentLength;
		try {
			contentLength = Long.parseLong(header.getValue());
		} catch (Exception e) {
			rep.carp(loc, "Invalid %s: %s", header, e.getMessage());
			return;
		}
		if (contentLength == 0) {
			rep.carp(loc, "Zero-sized body: %s", header);
		}
	}
}
