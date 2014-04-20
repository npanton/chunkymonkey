package uk.co.badgersinfoil.chunkymonkey.hls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.http.HttpResponse;
import uk.co.badgersinfoil.chunkymonkey.Locator;

public interface HttpResponseChecker {

	HttpResponseChecker NULL = new HttpResponseChecker() {
		@Override
		public void check(Locator loc, HttpResponse resp) { }
	};
	public static class Multi implements HttpResponseChecker {
		private List<HttpResponseChecker> entries = new ArrayList<HttpResponseChecker>();

		public Multi(HttpResponseChecker... checks) {
			Collections.addAll(entries, checks);
		}
		@Override
		public void check(Locator loc, HttpResponse resp) {
			for (HttpResponseChecker c : entries) {
				c.check(loc, resp);
			}
		}
	}

	void check(Locator loc, HttpResponse resp);
}
