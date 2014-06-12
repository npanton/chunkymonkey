package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.h264.SliceType.SliceName;

public enum SliceType {
	TYPE0(0, SliceName.P, false),
	TYPE1(1, SliceName.B, false),
	TYPE2(2, SliceName.I, false),
	TYPE3(3, SliceName.SP, false),
	TYPE4(4, SliceName.SI, false),
	TYPE5(5, SliceName.P, true),
	TYPE6(6, SliceName.B, true),
	TYPE7(7, SliceName.I, true),
	TYPE8(8, SliceName.SP, true),
	TYPE9(9, SliceName.SI, true);

	public static enum SliceName {
		P, B, I, SP, SI
	}

	private SliceName name;
	private int type;
	private boolean only;

	private SliceType(int type, SliceName name, boolean common) {
		this.type = type;
		this.name = name;
		this.only = common;
	}

	@Override
	public String toString() {
		return only ? name+"-only" : name.toString();
	}

	public static SliceType fromValue(int type) {
		switch (type) {
		case 0: return TYPE0;
		case 1: return TYPE1;
		case 2: return TYPE2;
		case 3: return TYPE3;
		case 4: return TYPE4;
		case 5: return TYPE5;
		case 6: return TYPE6;
		case 7: return TYPE7;
		case 8: return TYPE8;
		case 9: return TYPE9;
		default: throw new IllegalArgumentException("Invalid type "+type);
		}
	}

	public SliceName sliceName() {
		return name;
	}
}
