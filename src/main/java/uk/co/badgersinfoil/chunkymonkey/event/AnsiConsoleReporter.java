package uk.co.badgersinfoil.chunkymonkey.event;

import uk.co.badgersinfoil.chunkymonkey.event.Reporter.Event;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;

public class AnsiConsoleReporter implements Reporter {

	private EventFormatter formatter = new EventFormatter();

	@Override
	@Deprecated
	public void carp(Locator locator, String fmt, Object... args) {
		if (locator == null) {
			throw new IllegalArgumentException("locator must not be null");
		}
		long ts = System.currentTimeMillis();
		// TODO: serialise console output from multiple concurrent
		//       threads
		System.err.print("\033[36m");
		System.err.print(String.format("%tF %tT: ", ts, ts));
		System.err.print("\033[39m");
		System.err.println(String.format(fmt, args));
		System.err.println("\033[37m  [using deprecated Reporter API]");
		System.err.println("  at "+locator+"\033[39m");
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
		System.err.print("\033[36m");
		System.err.print(String.format("%tF %tT: ", ts, ts));
		System.err.print("\033[39m");
		System.err.println(formatter.format(event, logFormat));
		System.err.println("\033[37m  at "+event.locator()+"\033[39m");
	}
}
