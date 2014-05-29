package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.util.Arrays;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public class ContentLengthCheck implements HttpResponseChecker {

	private Reporter rep;

	public ContentLengthCheck(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void check(MediaContext mctx, HttpResponse resp, HttpClientContext ctx) {
		Header header = (Header)ctx.getAttribute(ContentLengthSnarfer.ORIGINAL_CONTENT_LENGTH);
		if (header == null) {
			rep.carp(mctx.getLocator(), "Response lacks 'Content-Length' header: %s", Arrays.toString(resp.getAllHeaders()));
			return;
		}
		long contentLength;
		try {
			contentLength = Long.parseLong(header.getValue());
		} catch (Exception e) {
			rep.carp(mctx.getLocator(), "Invalid %s: %s", header, e.getMessage());
			return;
		}
		if (contentLength == 0) {
			rep.carp(mctx.getLocator(), "Zero-sized body: %s", header);
		}
	}
}
