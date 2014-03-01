package uk.co.badgersinfoil.chunkymonkey;

public class ConsoleReporter implements Reporter {

	@Override
	public void carp(Locator locator, String fmt, Object... args) {
		if (locator == null) {
			throw new IllegalArgumentException("locator must not be null");
		}
		long ts = System.currentTimeMillis();
		System.err.print(String.format("%tF %tT: ", ts, ts));
		System.err.println(String.format(fmt, args));
		System.err.println("  at "+locator);
	}
}
