package uk.co.badgersinfoil.chunkymonkey.conformist;

import org.apache.http.Header;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.Reporter;

public class ContentLengthCheck extends AbstractSingleHeaderCheck {

	public ContentLengthCheck(Reporter rep) {
		super("Content-Length", rep);
	}

	@Override
	protected void checkSingleHeaderValue(Locator loc, Header header) {
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
