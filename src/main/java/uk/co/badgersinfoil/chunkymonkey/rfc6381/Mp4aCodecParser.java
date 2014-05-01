package uk.co.badgersinfoil.chunkymonkey.rfc6381;

import java.util.HashMap;
import java.util.Map;

public class Mp4aCodecParser implements CodecParser {

	public static class UnknownAudioCodec implements UnknownCodec {

		private String rest;

		public UnknownAudioCodec(String rest) {
			this.rest = rest;
		}
		@Override
		public String toString() {
			if (rest == null) {
				return "mp4a";
			}
			return "mp4a."+rest;
		}
	}

	private static final OtiParser UNKNOWN_INSTANCE = new OtiParser() {
		@Override
		public Rfc6381Codec parse(String rest) {
			return new UnknownAudioCodec(rest);
		}
	};
	private Map<String, OtiParser> otiParsers = new HashMap<String, OtiParser>();

	public void addParser(String oti, OtiParser parser) {
		otiParsers.put(oti, parser);
	}

	@Override
	public Rfc6381Codec parse(String codec) {
		if (!codec.startsWith("mp4a.")) {
			throw new IllegalArgumentException("prefix should be 'mp4a.': " + codec);
		}
		int pos = codec.indexOf('.', 5);
		String oti;
		String rest;
		if (pos == -1) {
			oti = codec.substring(5);
			rest = null;
		} else {
			oti = codec.substring(5, pos);
			rest = codec.substring(pos+1);
		}
		return findOtiParser(oti).parse(rest);
	}

	private OtiParser findOtiParser(String oti) {
		OtiParser parser = otiParsers.get(oti);
		if (parser == null) {
			return UNKNOWN_INSTANCE;
		}
		return parser;
	}
}
