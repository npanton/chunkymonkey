package uk.co.badgersinfoil.chunkymonkey.conformist.api;

import java.io.IOException;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMasterPlaylistContext;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMediaPlaylistContext;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class HlsMasterPlaylistContextSerializer extends
		JsonSerializer<HlsMasterPlaylistContext> {

	@Override
	public void serialize(HlsMasterPlaylistContext ctx,
			JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException
	{
		jgen.writeStartObject();
		jgen.writeObjectFieldStart("manifest");
		jgen.writeObjectField("specified", ctx.getManifestSpecified());
		jgen.writeObjectField("location", ctx.getManifestLocation());
		jgen.writeNumberField("last_updated", ctx.lastUpdated);
		jgen.writeEndObject();
		jgen.writeArrayFieldStart("media");
		for (HlsMediaPlaylistContext media : ctx.mediaContexts.values()) {
			jgen.writeObject(media);
		}
		jgen.writeEndArray();
		jgen.writeEndObject();
	}

}
