package uk.co.badgersinfoil.chunkymonkey.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;

public interface Reporter {
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	public @interface LogFormat {
		String value();
	}

	public abstract class Event {
		private Map<String, Object> attrs = new HashMap<String, Object>();
		private MediaContext ctx;
		private static final Pattern NAME = Pattern.compile("[\\p{Alpha}_]+[\\p{Alpha}\\p{Digit}_]*");

		public Event with(String name, Object value) {
			if (!NAME.matcher(name).matches()) {
				throw new IllegalArgumentException("bad name: "+name);
			}
			if (value == null) {
				throw new IllegalArgumentException("value must not be null");
			}
			attrs.put(name, value);
			return this;
		}

		public Event at(MediaContext ctx) {
			this.ctx = ctx;
			return this;
		}

		public void to(Reporter rep) {
			rep.carp(this);
		}

		public Object attr(String name) {
			return attrs.get(name);
		}
		public Map<String, Object> attrs() {
			return Collections.unmodifiableMap(attrs);
		}

		public Locator locator() {
			return ctx.getLocator();
		}
	}

	public static final Reporter NULL = new Reporter() {
		@Override
		public void carp(Event event) { }
		@Override
		public void carp(Locator locator, String format, Object... values) { }
	};

	void carp(Event event);

	@Deprecated
	void carp(Locator locator, String format, Object... values);
}
