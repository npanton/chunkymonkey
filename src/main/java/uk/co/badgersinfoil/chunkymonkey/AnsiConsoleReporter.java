package uk.co.badgersinfoil.chunkymonkey;

public class AnsiConsoleReporter implements Reporter {

	@Override
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
		System.err.println("\033[37m  at "+locator+"\033[39m");
	}
}
