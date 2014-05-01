package uk.co.badgersinfoil.chunkymonkey.h264;

public enum H264Profile {
	BASELINE(66),
	MAIN(77),
	EXTENDED(88),
	HIGH(100);

	private int profile_idc;

	private H264Profile(int profile_idc) {
		this.profile_idc = profile_idc;
	}

	public int getIndex() {
		return profile_idc;
	}

	public static H264Profile forIndex(int i) {
		switch (i) {
		case 66: return BASELINE;
		case 77: return MAIN;
		case 88: return EXTENDED;
		case 100: return HIGH;
		default: throw new IllegalArgumentException("Unsupported profile index: "+i);
		}
	}
}
