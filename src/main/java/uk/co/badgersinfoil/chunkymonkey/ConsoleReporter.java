package uk.co.badgersinfoil.chunkymonkey;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.co.badgersinfoil.chunkymonkey.event.EventFormatter;

public class ConsoleReporter implements Reporter {

	private EventFormatter formatter = new EventFormatter();

	/**
	 * Use {@link #carp(Event)} instead.
	 */
	@Override
	@Deprecated
	public void carp(Locator locator, String fmt, Object... args) {
		if (locator == null) {
			throw new IllegalArgumentException("locator must not be null");
		}
		long ts = System.currentTimeMillis();
		// TODO: serialise console output from multiple concurrent
		//       threads
		System.err.print(String.format("%tF %tT: ", ts, ts));
		System.err.println(String.format(fmt, args));
		System.err.println("  [using deprecated Reporter API]");
		System.err.println("  at "+locator);
	}

	@Override
	public void carp(Event event) {
		LogFormat logFormat = event.getClass().getAnnotation(LogFormat.class);
		if (logFormat == null) {
			throw new IllegalArgumentException(event.getClass().getName()+" fails to provide an "+LogFormat.class.getName()+" annotation");
		}
		long ts = System.currentTimeMillis();
		// TODO: serialise console output from multiple concurrent
		//       threads
		System.err.print(String.format("%tF %tT: ", ts, ts));
		System.err.println(formatter.format(event, logFormat));
		System.err.println("  at "+event.locator());
	}
}
