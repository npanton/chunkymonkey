package uk.co.badgersinfoil.chunkymonkey.rfc6381;

import uk.co.badgersinfoil.chunkymonkey.ts.AudioObjectType;

public class AudioObjectTypeCodec implements Rfc6381Codec {

	private AudioObjectType type;

	public AudioObjectTypeCodec(AudioObjectType type) {
		this.type = type;
	}

	public AudioObjectType getType() {
		return type;
	}

	@Override
	public String toString() {
		return "mp4a.40."+type.getIndex();
	}
}
