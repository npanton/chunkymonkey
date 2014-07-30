package uk.co.badgersinfoil.chunkymonkey.event;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface Reporter extends Closeable {
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	public @interface LogFormat {
		String value();
	}

	public static final Reporter NULL = new Reporter() {
		@Override
		public void carp(Event event) { }
		@Override
		public void carp(Locator locator, String format, Object... values) { }
		@Override
		public void close() throws IOException { }
	};

	void carp(Event event);

	@Deprecated
	void carp(Locator locator, String format, Object... values);
}
