package uk.co.badgersinfoil.chunkymonkey.event;

import java.io.IOException;
import org.hamcrest.Matcher;

public class FilterReporter implements Reporter {

	private Reporter delegate;
	private Matcher matcher;

	// TODO: a hamcrest Matcher may not be able to support all the context
	//       we would need to e.g. suppress multiple instances of the same
	//       event occurring within a specific (parent) MediaContext

	public FilterReporter(Reporter delegate, Matcher matcher) {
		this.delegate = delegate;
		this.matcher = matcher;
	}

	@Override
	public void carp(Event event) {
		if (matcher.matches(event)) {
			delegate.carp(event);
		}
	}

	@Override
	@Deprecated
	public void carp(Locator locator, String format, Object... values) {
		// no way to perform filtering
		delegate.carp(locator, format, values);
	}

	/**
	 * Closes the delegate Reporter instance given at construction time.
	 */
	@Override
	public void close() throws IOException {
		delegate.close();
	}
}
