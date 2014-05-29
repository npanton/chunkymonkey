package uk.co.badgersinfoil.chunkymonkey;

public interface MediaContext {
	/**
	 * Returns a Locator implementation representing the current this
	 * MediaContext's progress through the media.  The locator is likely
	 * to have 'parent' locators that describe the state of any parent
	 * MediaContext notionally enclosing this one.
	 */
	Locator getLocator();
}
