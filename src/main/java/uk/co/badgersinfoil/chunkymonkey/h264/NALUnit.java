package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.event.Locator;

public class NALUnit {

	public static enum UnitType {
		UNSPECIFIED(0),
		SLICE_LAYER_WITHOUT_PARTITIONING_NON_IDR(1),
		SLICE_DATA_PARTITION_A_LAYER(2),
		SLICE_DATA_PARTITION_B_LAYER(3),
		SLICE_DATA_PARTITION_C_LAYER(4),
		SLICE_LAYER_WITHOUT_PARTITIONING_IDR(5),
		SEI(6),
		SEQ_PARAMETER_SET(7),
		PIC_PARAMETER_SET(8),
		ACCESS_UNIT_DELIMITER(9),
		END_OF_SEQ(10),
		END_OF_STREAM(11),
		FILLER_DATA(12),
		RESERVED_13(13),
		RESERVED_14(14),
		RESERVED_15(15),
		RESERVED_16(16),
		RESERVED_17(17),
		RESERVED_18(18),
		RESERVED_19(19),
		RESERVED_20(20),
		RESERVED_21(21),
		RESERVED_22(22),
		RESERVED_23(23),
		UNSPECIFIED_24(24),
		UNSPECIFIED_25(25),
		UNSPECIFIED_26(26),
		UNSPECIFIED_27(27),
		UNSPECIFIED_28(28),
		UNSPECIFIED_29(29),
		UNSPECIFIED_30(30),
		UNSPECIFIED_31(31);

		private int id;

		private UnitType(int id) {
			this.id = id;
		}

		public static UnitType forId(int id) {
			switch (id) {
			case 0: return UNSPECIFIED;
			case 1: return SLICE_LAYER_WITHOUT_PARTITIONING_NON_IDR;
			case 2: return SLICE_DATA_PARTITION_A_LAYER;
			case 3: return SLICE_DATA_PARTITION_B_LAYER;
			case 4: return SLICE_DATA_PARTITION_C_LAYER;
			case 5: return SLICE_LAYER_WITHOUT_PARTITIONING_IDR;
			case 6: return SEI;
			case 7: return SEQ_PARAMETER_SET;
			case 8: return PIC_PARAMETER_SET;
			case 9: return ACCESS_UNIT_DELIMITER;
			case 10: return END_OF_SEQ;
			case 11: return END_OF_STREAM;
			case 12: return FILLER_DATA;
			case 13: return RESERVED_13;
			case 14: return RESERVED_14;
			case 15: return RESERVED_15;
			case 16: return RESERVED_16;
			case 17: return RESERVED_17;
			case 18: return RESERVED_18;
			case 19: return RESERVED_19;
			case 20: return RESERVED_20;
			case 21: return RESERVED_21;
			case 22: return RESERVED_22;
			case 23: return RESERVED_23;
			case 24: return UNSPECIFIED_24;
			case 25: return UNSPECIFIED_25;
			case 26: return UNSPECIFIED_26;
			case 27: return UNSPECIFIED_27;
			case 28: return UNSPECIFIED_28;
			case 29: return UNSPECIFIED_29;
			case 30: return UNSPECIFIED_30;
			case 31: return UNSPECIFIED_31;
			default: throw new IllegalArgumentException("Invalid id "+id);
			}
		}

		public int getId() {
			return id;
		}
	}

	private int header;

	public NALUnit(int header) {
		this.header = header;
	}

	public int forbiddenZeroBit() {
		return (header & 0b10000000) >> 7;
	}
	public int nalRefIdc() {
		return (header & 0b01100000) >> 5;
	}
	public UnitType nalUnitType() {
		return UnitType.forId((header & 0b00011111));
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("forbiddenZeroBit=").append(forbiddenZeroBit())
		 .append(" nalRefIdc=").append(nalRefIdc())
		 .append(" nalUnitType=").append(nalUnitType());
		return b.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + header;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NALUnit other = (NALUnit)obj;
		if (header != other.header)
			return false;
		return true;
	}
}
