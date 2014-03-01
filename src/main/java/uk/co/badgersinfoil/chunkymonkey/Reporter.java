package uk.co.badgersinfoil.chunkymonkey;

public interface Reporter {
	public static final Reporter NULL = new Reporter() {
		@Override
		public void carp(Locator locator, String string, Object... args) { }
	};

	void carp(Locator locator, String string, Object... args);
}
