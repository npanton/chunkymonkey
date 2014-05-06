package uk.co.badgersinfoil.chunkymonkey.conformist.api;

import org.eclipse.jetty.server.Server;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.module.SimpleModule;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMasterPlaylistContext;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMediaPlaylistContext;

public class ServerBuilder {
	private int port = 8080;
	private HlsMasterPlaylistContext ctx;

	private ServerBuilder() {
	}

	public static ServerBuilder create(HlsMasterPlaylistContext ctx) {
		ServerBuilder b = new ServerBuilder();
		b.ctx = ctx;
		return b;
	}

	public ServerBuilder port(int port) {
		this.port = port;
		return this;
	}

	private ObjectMapper buildMapper() {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule mod = new SimpleModule("SimpleModule",
                                new Version(1,0,0,null));
		mod.addSerializer(HlsMasterPlaylistContext.class, new HlsMasterPlaylistContextSerializer());
		mod.addSerializer(HlsMediaPlaylistContext.class, new HlsMediaPlaylistContextSerializer());
		mapper.registerModule(mod);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
		return mapper;
	}


	public Server build() {
		Server server = new Server(port);
		server.setHandler(createApiHandler());
		return server;
	}

	private ConformistApiHandler createApiHandler() {
		return new ConformistApiHandler(ctx, buildMapper());
	}
}
