package uk.co.badgersinfoil.chunkymonkey.aac;

import uk.co.badgersinfoil.chunkymonkey.aac.AacBlock.BlockType;


public enum ChannelConfiguration {
	OBJECT_TYPE_SPECIFIC_CONFIG(0, -1, null),
	MONO(1, 1, new BlockType[] { BlockType.SCE }),
	STEREO(2, 2, new BlockType[] { BlockType.CPE }),
	THREE(3, 3, new BlockType[] { BlockType.SCE, BlockType.CPE }),
	FOUR(4, 4, new BlockType[] { BlockType.SCE, BlockType.CPE, BlockType.SCE }),
	FIVE(5, 5, new BlockType[] { BlockType.SCE, BlockType.CPE, BlockType.CPE }),
	FIVE_ONE(6, 6, new BlockType[] { BlockType.SCE, BlockType.CPE, BlockType.CPE, BlockType.LFE }),
	SEVEN_ONE(7, 8, new BlockType[] { BlockType.SCE, BlockType.CPE, BlockType.CPE, BlockType.CPE, BlockType.LFE }),
	RESERVED_8(8, -1, null),
	RESERVED_9(9, -1, null),
	RESERVED_10(10, -1, null),
	RESERVED_11(11, -1, null),
	RESERVED_12(12, -1, null),
	RESERVED_13(13, -1, null),
	RESERVED_14(14, -1, null),
	RESERVED_15(15, -1, null),
	RESERVED_16(16, -1, null),
	RESERVED_17(17, -1, null),
	RESERVED_18(18, -1, null),
	RESERVED_19(19, -1, null),
	RESERVED_20(20, -1, null),
	RESERVED_21(21, -1, null),
	RESERVED_22(22, -1, null),
	RESERVED_23(23, -1, null),
	RESERVED_24(24, -1, null),
	RESERVED_25(25, -1, null),
	RESERVED_26(26, -1, null),
	RESERVED_27(27, -1, null),
	RESERVED_28(28, -1, null),
	RESERVED_29(29, -1, null),
	RESERVED_30(30, -1, null),
	RESERVED_31(31, -1, null);	

	private int index;
	private int speakers;
	private BlockType[] syntaxElements;

	private ChannelConfiguration(int index, int speakers, BlockType[] syntaxElements) {
		this.index = index;
		this.speakers = speakers;
		this.syntaxElements = syntaxElements;
	}
	
	public int getIndex() {
		return index;
	}
	public int getSpeakers() {
		return speakers;
	}
	public BlockType[] getSyntaxElements() {
		return syntaxElements;
	}

	public static ChannelConfiguration forIndex(int index) {
		switch (index) {
		case 0: return OBJECT_TYPE_SPECIFIC_CONFIG;
		case 1: return MONO;
		case 2: return STEREO;
		case 3: return THREE;
		case 4: return FOUR;
		case 5: return FIVE;
		case 6: return FIVE_ONE;
		case 7: return SEVEN_ONE;
		// TODO and the rest
		default: throw new IllegalArgumentException("Invalid index: "+index);
		}
	}
}
