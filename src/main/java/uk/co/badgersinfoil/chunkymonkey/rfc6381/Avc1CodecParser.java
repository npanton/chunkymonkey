package uk.co.badgersinfoil.chunkymonkey.rfc6381;

import uk.co.badgersinfoil.chunkymonkey.h264.H264Profile;

public class Avc1CodecParser implements CodecParser {

	public class UnknownAvc1Codec implements UnknownCodec {

		private String codec;

		public UnknownAvc1Codec(String codec) {
			this.codec = codec;
		}

		@Override
		public String toString() {
			return codec;
		}
	}

	@Override
	public Rfc6381Codec parse(String codec) {
		if (!codec.startsWith("avc1.")) {
			throw new IllegalArgumentException("prefix should be 'avc1.': " + codec);
		}
		int pos = codec.indexOf('.', 5);
		int profile_idc;
		Integer constraints;
		int level;
		try {
			if (pos == -1) {
				profile_idc = Integer.parseInt(codec.substring(5, 7), 16);
				constraints = Integer.valueOf(codec.substring(5, 7), 16);
				level = Integer.parseInt(codec.substring(5, 7), 16);
			} else {
				profile_idc = Integer.parseInt(codec.substring(5, pos));
				constraints = null;
				level = Integer.parseInt(codec.substring(pos+1));
			}
			H264Profile profile = H264Profile.forIndex(profile_idc);
			return new Avc1Codec(profile, constraints, level);
		} catch (Exception e) {
			return new UnknownAvc1Codec(codec);
		}
	}

}
