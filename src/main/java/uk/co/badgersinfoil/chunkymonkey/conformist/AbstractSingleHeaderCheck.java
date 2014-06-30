package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.util.Arrays;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.Reporter.Event;
import uk.co.badgersinfoil.chunkymonkey.Reporter.LogFormat;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;

public abstract class AbstractSingleHeaderCheck implements HttpResponseChecker {

	@LogFormat("There should not be multiple '{headerName}' headers: {headerList}")
	public static final class UnexpectedMultipleHeaderEvent extends Event { }
	@LogFormat("Response header missing '{headerName}'")
	public static final class MissingHeaderEvent extends Event { }

	private String headerName;
	protected Reporter rep;

	public AbstractSingleHeaderCheck(String headerName, Reporter rep) {
		this.headerName = headerName;
		this.rep = rep;
	}

	@Override
	public void check(MediaContext mctx, HttpResponse resp, HttpClientContext ctx) {
		Header[] headers = resp.getHeaders(headerName);
		if (headers.length > 1) {
			new UnexpectedMultipleHeaderEvent()
				.with("headerName", headerName)
				.with("headerList", Arrays.toString(headers))
				.at(mctx)
				.to(rep);
		} else if (headers.length == 0) {
			new MissingHeaderEvent()
				.with("headerName", headerName)
				.at(mctx)
				.to(rep);
		} else {
			checkSingleHeaderValue(mctx, headers[0]);
		}
	}

	protected abstract void checkSingleHeaderValue(MediaContext ctx, Header header);
}
