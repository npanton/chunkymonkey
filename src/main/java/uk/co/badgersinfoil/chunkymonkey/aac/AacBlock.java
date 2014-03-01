package uk.co.badgersinfoil.chunkymonkey.aac;

public abstract class AacBlock {
	public static enum BlockType {
		SCE(0), CPE(1), CCE(2), LFE(3), DSE(4), PCE(5), FIL(6), END(7);
		
		private int typeVal;

		private BlockType(int typeVal) {
			this.typeVal = typeVal;
		}
		
		public static BlockType valueOf(int typeVal) {
			switch (typeVal) {
			case 0: return SCE;
			case 1: return CPE;
			case 2: return CCE;
			case 3: return LFE;
			case 4: return DSE;
			case 5: return PCE;
			case 6: return FIL;
			case 7: return END;

			// Shouldn't trigger for invalid bitstreams, as
			// the field in the bitstream is only 3 bits long,
			default: throw new IllegalArgumentException("Invalid typeVal "+typeVal);
			}
		}
	}
}
