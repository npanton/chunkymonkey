package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;

/**
 * ResponseContentEncoding interceptor will remove Content-Length if the
 * response is decompressed, so take a copy of the header value
 */
public class ContentLengthSnarfer implements HttpResponseInterceptor {

	public static final String ORIGINAL_CONTENT_LENGTH = "original.content-length";

	@Override
	public void process(HttpResponse response, HttpContext context)
			throws HttpException, IOException
	{
		if (response.containsHeader("Content-Length")) {
			context.setAttribute(ORIGINAL_CONTENT_LENGTH,
			                     response.getLastHeader("Content-Length"));
		}
	}
}
