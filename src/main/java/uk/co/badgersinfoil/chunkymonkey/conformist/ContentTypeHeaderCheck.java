package uk.co.badgersinfoil.chunkymonkey.conformist;

import org.apache.http.Header;
import org.apache.http.HeaderElement;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Alert;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;

public class ContentTypeHeaderCheck extends AbstractSingleHeaderCheck {

	@LogFormat("Header lacks a value: '{header}'")
	public static final class MissingHeaderValueEvent extends Alert {
	}

	@LogFormat("Expected '{headerName}' header to have value '{expectedValue}', but got '{retrievedValue}'")
	public static final class MissingExpectedHeaderValueEvent extends Alert {
	}

	private String expectedType;

	public ContentTypeHeaderCheck(String expectedType, Reporter rep) {
		super("Content-Type", rep);
		this.expectedType = expectedType;
	}

	@Override
	protected void checkSingleHeaderValue(MediaContext ctx, Header header) {
		HeaderElement[] elements = header.getElements();
		if (elements.length == 0) {
			// rep.carp(ctx.getLocator(), "Header lacks a value: '%s'", header);
			new MissingHeaderValueEvent().with("header", header).at(ctx).to(rep);
		} else if (!expectedType.equalsIgnoreCase(elements[0].getName())) {
			new MissingExpectedHeaderValueEvent().with("headerName", header.getName()).with("expectedValue", expectedType).with("retrievedValue", elements[0].getName()).at(ctx).to(rep);
			// rep.carp(ctx.getLocator(),
			// "Expected '%s' header to have value '%s', but got '%s'",
			// header.getName(), expectedType, elements[0].getName());
		}
	}
}
