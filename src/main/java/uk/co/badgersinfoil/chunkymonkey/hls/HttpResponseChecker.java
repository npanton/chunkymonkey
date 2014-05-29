package uk.co.badgersinfoil.chunkymonkey.hls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;

public interface HttpResponseChecker {

	HttpResponseChecker NULL = new HttpResponseChecker() {
		@Override
		public void check(MediaContext mctx, HttpResponse resp, HttpClientContext ctx) { }
	};
	public static class Multi implements HttpResponseChecker {
		private List<HttpResponseChecker> entries = new ArrayList<HttpResponseChecker>();

		public Multi(HttpResponseChecker... checks) {
			Collections.addAll(entries, checks);
		}
		@Override
		public void check(MediaContext mctx, HttpResponse resp, HttpClientContext ctx) {
			for (HttpResponseChecker c : entries) {
				c.check(mctx, resp, ctx);
			}
		}
	}

	void check(MediaContext mctx, HttpResponse resp, HttpClientContext ctx);
}
