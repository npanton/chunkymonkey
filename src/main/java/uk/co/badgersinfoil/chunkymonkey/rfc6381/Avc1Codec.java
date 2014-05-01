package uk.co.badgersinfoil.chunkymonkey.rfc6381;

import uk.co.badgersinfoil.chunkymonkey.h264.H264Profile;

public class Avc1Codec implements Rfc6381Codec {

	private H264Profile profile;
	private Integer constraints;
	private int level;

	public Avc1Codec(H264Profile profile, Integer constraints, int level) {
		this.profile = profile;
		this.constraints = constraints;
		this.level = level;
	}

	public H264Profile getProfile() {
		return profile;
	}
	public Integer getConstraints() {
		return constraints;
	}
	public int getLevel() {
		return level;
	}

	@Override
	public String toString() {
		if (constraints == null) {
			return "avc1."+profile.getIndex()+"."+level;
		}
		return String.format("avc1.%02x%02x%02x", profile.getIndex(), constraints, level);
	}
}
