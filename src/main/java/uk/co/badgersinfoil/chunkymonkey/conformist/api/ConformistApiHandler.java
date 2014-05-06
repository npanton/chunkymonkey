package uk.co.badgersinfoil.chunkymonkey.conformist.api;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMasterPlaylistContext;

public class ConformistApiHandler extends AbstractHandler {

	private HlsMasterPlaylistContext ctx;
	private ObjectMapper ser;

	public ConformistApiHandler(HlsMasterPlaylistContext ctx, ObjectMapper ser) {
		this.ctx = ctx;
		this.ser = ser;
	}

	@Override
	public void handle(String target,
	                   Request baseRequest,
	                   HttpServletRequest request,
	                   HttpServletResponse response)
		throws IOException, ServletException
	{
		response.setContentType("application/json;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
		ser.writeValue(response.getWriter(), ctx);
	}
}
