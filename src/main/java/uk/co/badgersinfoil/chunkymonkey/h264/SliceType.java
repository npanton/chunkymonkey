package uk.co.badgersinfoil.chunkymonkey.h264;

public enum SliceType {
	P(0),
	B(1),
	I(2),
	SP(3),
	SI(4),
	P_COMMON(5),
	B_COMMON(6),
	I_COMMON(7),
	SP_COMMON(8),
	SI_COMMON(9);

	private int type;

	private SliceType(int type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return name()+"("+type+")";
	}

	public static SliceType fromValue(int type) {
		switch (type) {
		case 0: return P;
		case 1: return B;
		case 2: return I;
		case 3: return SP;
		case 4: return SI;
		case 5: return P_COMMON;
		case 6: return B_COMMON;
		case 7: return I_COMMON;
		case 8: return SP_COMMON;
		case 9: return SI_COMMON;
		default: throw new IllegalArgumentException("Invalid type "+type);
		}
	}
}
