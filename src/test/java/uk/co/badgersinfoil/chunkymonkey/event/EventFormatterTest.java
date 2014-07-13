package uk.co.badgersinfoil.chunkymonkey.event;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.Event;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;


public class EventFormatterTest {

	@LogFormat("{a}b")
	public class TestEvent extends Event { }

	private EventFormatter f = new EventFormatter();

	@Test
	public void basic() {
		TestEvent event = new TestEvent();
		event.with("a", "A");
		LogFormat logFormat = event.getClass().getAnnotation(LogFormat.class);
		assertEquals("Ab", f.format(event, logFormat));
	}

	@Test
	public void missingPlaceholder() {
		TestEvent event = new TestEvent();
		event.with("a", "A")
		     .with("missing", "value");
		LogFormat logFormat = event.getClass().getAnnotation(LogFormat.class);
		assertThat(f.format(event, logFormat),
		           allOf(containsString("missing"),
		                 containsString("value")));
	}
}
