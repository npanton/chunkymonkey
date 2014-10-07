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
import uk.co.badgersinfoil.chunkymonkey.event.Alert;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public class CachingHeaderCheck implements HttpResponseChecker {

	@LogFormat("Response 'Expires' in {expirySeconds} seconds, but max-age is {maxAgeSeconds} seconds ({expiresHeader}, {dateHeader})")
	public static class ExpiresMaxAgeMissmatchEvent extends Alert {
	}

	@LogFormat("Max Age Problem, {maxAgeProblem}")
	public static class GeneralMaxAgeEvent extends Alert {
	}

	@LogFormat("Response 'Expires' in the past: {expiresHeader} vs {dateHeader}")
	public static class ResponseExpiresInThePastEvent extends Alert {
	}

	@LogFormat("Response contains 'Expires', but not 'Date' header: {expiresHeader}")
	public static class ResponseExpiresWithoutDateEvent extends Alert {
	}

	@LogFormat("Repeated max-age, last was {maxAgeValue}, this is {elValue}")
	public static class RepeatedMaxAgeEventEvent extends Alert {
	}

	@LogFormat("Bad cacheability class {elName} in: {header}")
	public static class BadCacheabilityEvent extends Alert {
	}

	private Reporter rep;
	private int minMaxAge;
	private static final Set<String> BAD_CLASSES = new HashSet<>();
	static {
		Collections.addAll(BAD_CLASSES, "private", "no-cache");
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
						// rep.carp(mctx.getLocator(),
						// "Repeated max-age, last was %d, this is %s",
						// maxAgeValue, el.getValue());
						new RepeatedMaxAgeEventEvent().with("maxAgeValue", maxAgeValue).with("elValue", el.getValue()).at(mctx).to(rep);
					}
				} else if (BAD_CLASSES.contains(el.getName().toLowerCase())) {
					// rep.carp(mctx.getLocator(),
					// "Bad cacheability class %s in: %s", el.getName(),
					// header);
					new BadCacheabilityEvent().with("header", header).with("elName", el.getName()).at(mctx).to(rep);
				}
			}
		}
		if (resp.containsHeader("Expires")) {
			Header expiresHeader = resp.getLastHeader("Expires");
			if (!resp.containsHeader("Date")) {
				// rep.carp(mctx.getLocator(),
				// "Response contains 'Expires', but not 'Date' header: %s",
				// expiresHeader);
				new ResponseExpiresWithoutDateEvent().with("expiresHeader", expiresHeader).at(mctx).to(rep);
			} else {
				Date expires = DateUtils.parseDate(expiresHeader.getValue());
				Header dateHeader = resp.getLastHeader("Date");
				Date date = DateUtils.parseDate(dateHeader.getValue());
				long max = expires.getTime() - date.getTime();
				if (max < 0) {
					// rep.carp(mctx.getLocator(),
					// "Response 'Expires' in the past: %s vs %s",
					// expiresHeader, dateHeader);
					new ResponseExpiresInThePastEvent().with("expiresHeader", expiresHeader).with("dateHeader", dateHeader).at(mctx).to(rep);
				} else {
					if (maxAgeValue != null && max / 1000 != maxAgeValue) {
						new ExpiresMaxAgeMissmatchEvent().with("expirySeconds", max / 1000).with("maxAgeSeconds", maxAgeValue).with("expiresHeader", expiresHeader).with("dateHeader", dateHeader)
								.at(mctx).to(rep);

					}
				}
			}
		}
		if (maxAgeValue == null) {
			// rep.carp(mctx.getLocator(), "no max-age specified");
			new GeneralMaxAgeEvent().with("maxAgeProblem", "no max-age specified").at(mctx).to(rep);
		} else if (maxAgeValue < minMaxAge) {
			// rep.carp(mctx.getLocator(),
			// "max-age is too small: %d (at least %d required)", maxAgeValue,
			// minMaxAge);
			new GeneralMaxAgeEvent().with("maxAgeProblem", String.format("max-age is too small: %d (at least %d required)", maxAgeValue, minMaxAge)).at(mctx).to(rep);

		}
	}
}
