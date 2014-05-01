package uk.co.badgersinfoil.chunkymonkey.rfc6381;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodecsParser {
	private static class UnknownCodecImpl implements UnknownCodec {

		private String codec;

		public UnknownCodecImpl(String codec) {
			this.codec = codec;
		}
		@Override
		public String toString() {
			return codec;
		}
	}

	private static class UnknownCodecParser implements CodecParser {

		@Override
		public Rfc6381Codec parse(String codec) {
			return new UnknownCodecImpl(codec);
		}
	}

	private Map<String,CodecParser> parsers = new HashMap<>();

	private UnknownCodecParser UNKNOWN_INSTANCE = new UnknownCodecParser();

	public void addParser(String prefix, CodecParser parser) {
		parsers.put(prefix, parser);
	}

	public List<Rfc6381Codec> parseCodecs(String codecs) {
		// TODO: not terribly robust,
		String[] strings = codecs.split(",");
		List<Rfc6381Codec> result = new ArrayList<Rfc6381Codec>(strings.length);
		for (String s : strings) {
			result.add(parseCodec(s));
		}
		return result;
	}


	private Rfc6381Codec parseCodec(String codec) {
		return findCodecParser(prefix(codec.trim())).parse(codec);
	}


	private static String prefix(String s) {
		int pos = s.indexOf('.');
		if (pos == -1) {
			return s;
		}
		return s.substring(0, pos);
	}


	private CodecParser findCodecParser(String c) {
		CodecParser codecParser = parsers.get(c);
		if (codecParser == null) {
			return UNKNOWN_INSTANCE;
		}
		return codecParser;
	}
}
