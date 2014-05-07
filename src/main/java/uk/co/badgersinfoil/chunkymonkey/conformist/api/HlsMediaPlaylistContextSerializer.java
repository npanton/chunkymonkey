package uk.co.badgersinfoil.chunkymonkey.conformist.api;

import java.io.IOException;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMediaPlaylistContext;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class HlsMediaPlaylistContextSerializer extends
		JsonSerializer<HlsMediaPlaylistContext> {

	@Override
	public void serialize(HlsMediaPlaylistContext ctx,
	                      JsonGenerator jgen,
	                      SerializerProvider provider)
		throws IOException, JsonProcessingException
	{
		jgen.writeStartObject();
		if (ctx.lastMediaSequence != null) {
			jgen.writeNumberField("last_processed_media_sequence", ctx.lastProcessedMediaSeq());
		}
		if (ctx.lastMediaSequence != null) {
			jgen.writeNumberField("target_duration", ctx.lastTargetDuration);
		}

		if (ctx.getPlaylistInfo() != null) {
			jgen.writeObjectFieldStart("master_manifest_info");
			if (ctx.getPlaylistInfo().getBandWitdh() != -1) {
				jgen.writeNumberField("bandwidth", ctx.getPlaylistInfo().getBandWitdh());
			}
			if (ctx.getPlaylistInfo().getResolution() != null) {
				jgen.writeStringField("resolution", ctx.getPlaylistInfo().getResolution());
			}
			if (ctx.getPlaylistInfo().getCodecs() != null) {
				jgen.writeStringField("codecs", ctx.getPlaylistInfo().getCodecs());
			}
			jgen.writeEndObject();
		}

		jgen.writeObjectFieldStart("media_manifest");
		jgen.writeObjectField("uri", ctx.manifest);
		jgen.writeObjectField("http_stats", ctx.playlistStats);
		jgen.writeEndObject();

		jgen.writeObjectFieldStart("media_segments");
		jgen.writeObjectField("http_stats", ctx.segmentStats);
		jgen.writeEndObject();

		jgen.writeEndObject();
	}

}
