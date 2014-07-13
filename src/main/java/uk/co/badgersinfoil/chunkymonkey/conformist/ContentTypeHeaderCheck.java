package uk.co.badgersinfoil.chunkymonkey.conformist;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;

public class ContentTypeHeaderCheck extends AbstractSingleHeaderCheck {

	private String expectedType;

	public ContentTypeHeaderCheck(String expectedType, Reporter rep) {
		super("Content-Type", rep);
		this.expectedType = expectedType;
	}

	@Override
	protected void checkSingleHeaderValue(MediaContext ctx, Header header) {
		HeaderElement[] elements = header.getElements();
		if (elements.length == 0) {
			rep.carp(ctx.getLocator(), "Header lacks a value: '%s'", header);
		} else if (!expectedType.equalsIgnoreCase(elements[0].getName())) {
			rep.carp(ctx.getLocator(), "Expected '%s' header to have value '%s', but got '%s'", header.getName(), expectedType, elements[0].getName());
		}
	}
}
