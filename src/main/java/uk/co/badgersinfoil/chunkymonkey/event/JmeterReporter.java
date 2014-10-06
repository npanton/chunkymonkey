package uk.co.badgersinfoil.chunkymonkey.event;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.beanutils.PropertyUtils;

import com.google.common.base.CaseFormat;

public class JmeterReporter implements Reporter {

	private EventFormatter formatter = new EventFormatter();
	private List<Map<String, Object>> events = new ArrayList<Map<String, Object>>();

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
		// threads
		System.err.print(String.format("%tF %tT: ", ts, ts));
		System.err.println(String.format(fmt, args));
		System.err.println("  [using deprecated Reporter API]");
		System.err.println("  at " + locator);
	}

	@Override
	public void carp(Event event) {
		LogFormat logFormat = event.getClass().getAnnotation(LogFormat.class);
		if (logFormat == null) {
			throw new IllegalArgumentException(event.getClass().getName() + " fails to provide an " + LogFormat.class.getName() + " annotation");
		}
		Map<String, Object> attributes = new HashMap<String, Object>();
		for (Entry<String, Object> attr : event.attrs().entrySet()) {
			attributes.put(toAttrName(attr.getKey()), String.valueOf(attr.getValue()));
		}
		attributes.put("locator", event.locator());
		attributes.put("message", formatter.format(event, logFormat));
		attributes.put("event_name", toEventName(event));

		Locator loc = event.locator();
		while (loc != null) {
			Map<String, Object> aMap = addLocatorAttributes(loc);
			if (!aMap.isEmpty()) {
				attributes.putAll(aMap);
			}

			loc = loc.getParent();
			// System.out.println(loc);
		}
		events.add(attributes);

	}

	private String toAttrName(String name) {
		return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
	}

	private String toEventName(Event event) {
		String prefix = event.getClass().getPackage().getName().replaceAll("uk\\.co\\.badgersinfoil\\.chunkymonkey\\.", "");
		prefix = prefix.replaceAll("conformist\\.", "");
		String suffix = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, event.getClass().getSimpleName().replaceAll("(?:Perf|Event|Alert)$", ""));
		// return "conformist " + prefix + " " + suffix;

		String eventName = prefix.isEmpty() ? suffix : prefix + "." + suffix;

		return eventName;
	}

	private Map<String, Object> addLocatorAttributes(Locator loc) {
		String prefix = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, loc.getClass().getSimpleName().replaceAll("Locator$", ""));
		Map<String, Object> ev = new HashMap<String, Object>();

		try {
			Map<String, Object> map = PropertyUtils.describe(loc);
			for (Entry<String, Object> prop : map.entrySet()) {
				if (prop.getKey().equals("class") || prop.getKey().equals("parent")) {
					continue;
				}
				String suffix = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, prop.getKey());
				String name = prefix + "_" + suffix;
				int i = 1;
				while (ev.containsKey(name)) {
					name = prefix + i + "_" + suffix;
					i++;
				}
				ev.put(name, String.valueOf(prop.getValue()));
			}
		} catch (IllegalAccessException e1) {
		} catch (InvocationTargetException e1) {
		} catch (NoSuchMethodException e1) {
		}
		return ev;
	}

	public List<Map<String, Object>> getEvents() {
		return events;
	}

	@Override
	public void close() throws IOException {
	}

}
