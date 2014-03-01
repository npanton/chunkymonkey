package uk.co.badgersinfoil.chunkymonkey.aac;

public enum AacProfile {
	MAIN(0),
	LOW_COMPLEXITY(1),
	SCALABLE_SAMPLE_RATE(2),
	RESERVED_3(3);
	
	private int index;

	private AacProfile(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
	public static AacProfile forIndex(int index) {
		switch (index) {
		case 0: return MAIN;
		case 1: return LOW_COMPLEXITY;
		case 2: return SCALABLE_SAMPLE_RATE;
		case 3: return RESERVED_3;
		default: throw new IllegalArgumentException("Invalid index: "+index);
		}
	}
}
