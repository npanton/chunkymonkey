package uk.co.badgersinfoil.chunkymonkey.rfc6381;

import uk.co.badgersinfoil.chunkymonkey.ts.AudioObjectType;

public class AudioObjectTypeParser implements OtiParser {

	public class UnknownAudioObjectType implements Rfc6381Codec {

		public UnknownAudioObjectType(String rest) {
		}

	}

	@Override
	public Rfc6381Codec parse(String aot) {
		try {
			int t = Integer.parseInt(aot);
			AudioObjectType type = AudioObjectType.forIndex(t);
			return new AudioObjectTypeCodec(type);
		} catch (Exception e) {
			return new UnknownAudioObjectType(aot);
		}
	}

}
