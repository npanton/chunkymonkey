package uk.co.badgersinfoil.chunkymonkey.hls;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

public class HttpCondition {

	private String lastETag;
	private String lastLastModified;

	public void makeConditional(HttpRequest req)
	{
		if (lastETag != null) {
			req.setHeader("If-None-Match", lastETag);
		}
		if (lastLastModified != null) {
			req.setHeader("If-Modified-Since", lastLastModified);
		}
	}

	public void recordCacheValidators(HttpResponse resp)
	{
		if (resp.containsHeader("ETag")) {
			lastETag = resp.getLastHeader("ETag").getValue();
		} else {
			lastETag = null;
		}
		if (resp.containsHeader("Last-Modified")) {
			lastLastModified = resp.getLastHeader("Last-Modified").getValue();
		} else {
			lastLastModified = null;
		}
	}
}
