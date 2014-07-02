package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.DateUtils;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.Reporter.Event;
import uk.co.badgersinfoil.chunkymonkey.Reporter.LogFormat;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public class CachingHeaderCheck implements HttpResponseChecker {

	@LogFormat("Response 'Expires' in {expirySeconds} seconds, but max-age is {maxAgeSeconds} seconds ({expiresHeader}, {dateHeader})")
	public static class ExpiresMaxAgeMissmatchEvent extends Event { }

	private Reporter rep;
	private int minMaxAge;
	private static final Set<String> BAD_CLASSES = new HashSet<>();
	static {
		Collections.addAll(BAD_CLASSES,
			"private", "no-cache"
		);
	}

	public CachingHeaderCheck(Reporter rep, int minMaxAge) {
		this.rep = rep;
		this.minMaxAge = minMaxAge;
	}

	@Override
	public void check(MediaContext mctx, HttpResponse resp, HttpClientContext ctx) {
		Integer maxAgeValue = null;
		for (Header header : resp.getHeaders("Cache-Control")) {
			for (HeaderElement el : header.getElements()) {
				if (el.getName().equalsIgnoreCase("max-age")) {
					if (maxAgeValue == null) {
						maxAgeValue = Integer.valueOf(el.getValue());
					} else {
						rep.carp(mctx.getLocator(), "Repeated max-age, last was %d, this is %s", maxAgeValue, el.getValue());
					}
				} else if (BAD_CLASSES.contains(el.getName().toLowerCase())) {
					rep.carp(mctx.getLocator(), "Bad cacheability class %s in: %s", el.getName(), header);
				}
			}
		}
		if (resp.containsHeader("Expires")) {
			Header expiresHeader = resp.getLastHeader("Expires");
			if (!resp.containsHeader("Date")) {
				rep.carp(mctx.getLocator(), "Response contains 'Expires', but not 'Date' header: %s", expiresHeader);
			} else {
				Date expires = DateUtils.parseDate(expiresHeader.getValue());
				Header dateHeader = resp.getLastHeader("Date");
				Date date = DateUtils.parseDate(dateHeader.getValue());
				long max = expires.getTime() - date.getTime();
				if (max < 0) {
					rep.carp(mctx.getLocator(), "Response 'Expires' in the past: %s vs %s", expiresHeader, dateHeader);
				} else {
					if (maxAgeValue!= null && max / 1000 != maxAgeValue) {
						new ExpiresMaxAgeMissmatchEvent()
							.with("expirySeconds", max / 1000)
							.with("maxAgeSeconds", maxAgeValue)
							.with("expiresHeader", expiresHeader)
							.with("dateHeader", dateHeader)
							.at(mctx)
							.to(rep);
					}
				}
			}
		}
		if (maxAgeValue == null) {
			rep.carp(mctx.getLocator(), "no max-age specified");
		} else if (maxAgeValue < minMaxAge) {
			rep.carp(mctx.getLocator(), "max-age is too small: %d (at least %d required)", maxAgeValue, minMaxAge);
		}
	}

}
