package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.util.Arrays;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Alert;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public class ContentLengthCheck implements HttpResponseChecker {

	@LogFormat("Response lacks 'Content-Length' header: {allHeaders}")
	public static final class MissingContentLengthEvent extends Alert {
	}

	@LogFormat("Invalid {header}: {message}")
	public static final class InvalidContentLengthEvent extends Alert {
	}

	@LogFormat("Zero-sized body: {header}")
	public static final class ZeroSizedContentLengthEvent extends Alert {
	}

	private Reporter rep;

	public ContentLengthCheck(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void check(MediaContext mctx, HttpResponse resp, HttpClientContext ctx) {
		Header header = (Header) ctx.getAttribute(ContentLengthSnarfer.ORIGINAL_CONTENT_LENGTH);
		if (header == null) {
			// rep.carp(mctx.getLocator(),
			// "Response lacks 'Content-Length' header: %s",
			// Arrays.toString(resp.getAllHeaders()));
			new MissingContentLengthEvent().with("allHeaders", Arrays.toString(resp.getAllHeaders())).at(mctx).to(rep);
			return;
		}
		long contentLength;
		try {
			contentLength = Long.parseLong(header.getValue().trim());
		} catch (Exception e) {
			e.printStackTrace();
			// rep.carp(mctx.getLocator(), "Invalid %s: %s", header,
			// e.getMessage());
			new InvalidContentLengthEvent().with("header", header).with("message", e.getMessage()).at(mctx).to(rep);
			return;
		}
		if (contentLength == 0) {
			// rep.carp(mctx.getLocator(), "Zero-sized body: %s", header);
			new ZeroSizedContentLengthEvent().with("header", header).at(mctx).to(rep);
		}
	}
}
