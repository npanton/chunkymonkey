package uk.co.badgersinfoil.chunkymonkey.h264;

import java.util.Arrays;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.h264.PicParamSetNalUnitConsumer.PicParamSetNalUnitContext;
import uk.co.badgersinfoil.chunkymonkey.h264.SeqParamSet.ChromaFormat;
import io.netty.buffer.ByteBuf;

public class PicParamSet {

	public static class PicScalingMatrix {

		private ScalingList[] scalingList4x4 = new ScalingList[8];
		private ScalingList[] scalingList8x8 = new ScalingList[8];

		public static PicScalingMatrix parse(PicParamSetNalUnitContext ctx,
				H264BitBuf bits, boolean transform8x8Mode) {
			int max = 6;
			if (transform8x8Mode) {
				max += (ctx.getH264Context().lastSeqParamSet().chromaFormat() != ChromaFormat.YUV444) ? 2 : 6;
			}
			PicScalingMatrix result = new PicScalingMatrix();
			for (int i=0; i<max; i++) {
				boolean seqScalingListPresentFlag = bits.readBool();
				if (seqScalingListPresentFlag) {
					if (i < 6) {
						result.scalingList4x4[i] = new ScalingList(bits, 16);
					} else {
						result.scalingList8x8[i - 6] = new ScalingList(bits, 64);
					}
				}
			}
			return result;
		}

		public StringBuilder toString(StringBuilder b) {
			b.append("scallingList4x4=").append(Arrays.toString(scalingList4x4))
			 .append("scallingList8x8=").append(Arrays.toString(scalingList8x8));
			return b;
		}
		@Override
		public String toString() {
			return toString(new StringBuilder()).toString();
		}
	}

	public static enum WeightedBipred {
		DEFAULT,
		EXPLICIT,
		IMPLICIT;

		public static WeightedBipred forIndex(int idx) {
			switch (idx) {
			case 0: return DEFAULT;
			case 1: return EXPLICIT;
			case 2: return IMPLICIT;
			default: throw new IllegalArgumentException("Invalid index");
			}
		}

	}

	public static enum SliceGroupMapType {
		INTERLEAVED,
		DISPERSED,
		FOREGROUND_AND_LEFTOVER,
		CHANGING3,
		CHANGING4,
		CHANGING5,
		EXPLICIT_ASSIGNMENT;

		public static SliceGroupMapType forIndex(int i) {
			switch (i) {
			case 0: return INTERLEAVED;
			case 1: return DISPERSED;
			case 2: return FOREGROUND_AND_LEFTOVER;
			case 3: return CHANGING3;
			case 4: return CHANGING4;
			case 5: return CHANGING5;
			case 6: return EXPLICIT_ASSIGNMENT;
			default: throw new IllegalArgumentException("Invalid slice_group_map_type "+i);
			}
		}
	}

	public static enum EntropyCodingMode {
		EXP_GOLOMB, CABAC

	}

	private int picParameterSetId;
	private int seqParameterSetId;
	private EntropyCodingMode entropyCodingMode;
	private boolean picOrderPresent;
	private int numSliceGroupsMinus1;
	private SliceGroupMapType sliceGroupMapType;
	private int[] runLengthMinus1;
	private int[] topLeft;
	private int[] bottomRight;
	private boolean sliceGroupChangeDirection;
	private int sliceGroupChangeRateMinus1;
	private int picSizeInMapUnitsMinus1;
	private int[] sliceGroupId;
	private int numRefIdxL0ActiveMinus1;
	private int numRefIdxL1ActiveMinus1;
	private boolean weightedPred;
	private WeightedBipred weightedBipred;
	private int picInitQpMinus26;
	private int picInitQsMinus26;
	private int chromaQpIndexOffset;
	private boolean deblockingFilterControlPresent;
	private boolean constrainedIntraPred;
	private boolean redundantPicCntPresent;
	private boolean transform8x8Mode;
	private boolean picScalingMatrixPresent;

	// following not present for baseline profile,
	private PicScalingMatrix picScalingMatrix;
	private int secondChromaQpIndexOffset;

	private PicParamSet() { }

	public static PicParamSet parse(PicParamSetNalUnitContext ctx, ByteBuf buf, Reporter rep) {
		PicParamSet p = new PicParamSet();
		if (p.parse0(ctx, buf, rep)) {
			return p;
		}
		return null;
	}

	private boolean parse0(PicParamSetNalUnitContext ctx, ByteBuf buf, Reporter rep) {
		H264BitBuf bits = new H264BitBuf(buf);
		picParameterSetId = bits.readUE();
		seqParameterSetId = bits.readUE();
		if (seqParameterSetId != ctx.getH264Context().lastSeqParamSet().seqParamSetId()) {
			rep.carp(ctx.getLocator(), "pic_parameter_set id=%d: seq_param_set_id=%d does not match id of last seq_param_set (%d)", picParameterSetId, seqParameterSetId, ctx.getH264Context().lastSeqParamSet().seqParamSetId());
			return false;
		}
		entropyCodingMode = bits.readBool()
				? EntropyCodingMode.EXP_GOLOMB
				: EntropyCodingMode.CABAC;
		picOrderPresent = bits.readBool();
		numSliceGroupsMinus1 = bits.readUE();
		if (numSliceGroupsMinus1 > 0) {
			sliceGroupMapType = SliceGroupMapType.forIndex(bits.readUE());
			switch (sliceGroupMapType) {
			case INTERLEAVED:
				runLengthMinus1 = new int[numSliceGroupsMinus1+1];
				for (int g=0; g <= numSliceGroupsMinus1; g++) {
					runLengthMinus1[g] = bits.readUE();
				}
				break;
			case FOREGROUND_AND_LEFTOVER:
				topLeft = new int[numSliceGroupsMinus1+1];
				bottomRight = new int[numSliceGroupsMinus1+1];
				for (int g=0; g <= numSliceGroupsMinus1; g++) {
					topLeft[g] = bits.readUE();
					bottomRight[g] = bits.readUE();
				}
				break;
			case CHANGING3:
			case CHANGING4:
			case CHANGING5:
				sliceGroupChangeDirection = bits.readBool();
				sliceGroupChangeRateMinus1 = bits.readUE();
				break;
			case EXPLICIT_ASSIGNMENT:
				picSizeInMapUnitsMinus1 = bits.readUE();
				int size = (int)Math.ceil(Math.log(numSliceGroupsMinus1+1) / Math.log(2));
				sliceGroupId = new int[picSizeInMapUnitsMinus1+1];
				for (int i=0; i<=picSizeInMapUnitsMinus1; i++) {
					sliceGroupId[i] = bits.readBits(size);
				}
				break;
			}
		}
		numRefIdxL0ActiveMinus1 = bits.readUE();
		numRefIdxL1ActiveMinus1 = bits.readUE();
		weightedPred = bits.readBool();
		int weightedBipredIdc = bits.readBits(2);
		if (weightedBipredIdc > 2) {
			rep.carp(ctx.getLocator(), "weighted_bipred_idc must be 0-2: "+weightedBipredIdc);
			return false;
		}
		weightedBipred = WeightedBipred.forIndex(weightedBipredIdc);
		picInitQpMinus26 = bits.readSE();
		picInitQsMinus26 = bits.readSE();
		chromaQpIndexOffset = bits.readSE();
		deblockingFilterControlPresent = bits.readBool();
		constrainedIntraPred = bits.readBool();
		redundantPicCntPresent = bits.readBool();
		// TODO: should not be present for baseline profile,
		if (bits.moreRbspData()) {
			transform8x8Mode = bits.readBool();
			picScalingMatrixPresent = bits.readBool();
			if (picScalingMatrixPresent) {
				picScalingMatrix = PicScalingMatrix.parse(ctx, bits, transform8x8Mode);
			}
			secondChromaQpIndexOffset = bits.readSE();
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("PicParamSet [picParameterSetId=")
		 .append(picParameterSetId())
		 .append(", seqParameterSetId=")
		 .append(seqParameterSetId())
		 .append(", entropyCodingMode=")
		 .append(entropyCodingMode())
		 .append(", picOrderPresent=")
		 .append(picOrderPresent())
		 .append(", numSliceGroups=")
		 .append(numSliceGroups());
		if (numSliceGroups() > 1) {
			b.append(", sliceGroupMapType=")
			 .append(sliceGroupMapType());
			switch (sliceGroupMapType()) {
			case INTERLEAVED:
				b.append(", runLengthMinus1=")
				 .append(Arrays.toString(runLengthMinus1()));
				break;
			case FOREGROUND_AND_LEFTOVER:
				b.append(", topLeft=")
				 .append(Arrays.toString(topLeft()))
				 .append(", bottomRight=")
				 .append(Arrays.toString(bottomRight()));
				break;
			case CHANGING3:
			case CHANGING4:
			case CHANGING5:
				b.append(", sliceGroupChangeDirection=")
				 .append(sliceGroupChangeDirection())
				 .append(", sliceGroupChangeRate=")
				 .append(sliceGroupChangeRate());
				break;
			case EXPLICIT_ASSIGNMENT:
				b.append(", picSizeInMapUnits=")
				 .append(picSizeInMapUnits())
				 .append(", sliceGroupId=")
				 .append(Arrays.toString(sliceGroupId()));
				break;
			}
		}
		b.append(", numRefIdxL0Active=")
		 .append(numRefIdxL0Active())
		 .append(", numRefIdxL1Active=")
		 .append(numRefIdxL1Active())
		 .append(", weightedPred=").append(weightedPred())
		 .append(", weightedBipred=")
		 .append(weightedBipred())
		 .append(", picInitQp=")
		 .append(picInitQp())
		 .append(", picInitQs=")
		 .append(picInitQs())
		 .append(", chromaQpIndexOffset=")
		 .append(chromaQpIndexOffset());
		if (deblockingFilterControlPresent()) {
			b.append(", deblockingFilterControlPresent");
		}
		if (constrainedIntraPred()) {
			b.append(", constrainedIntraPred");
		}
		if (redundantPicCntPresent()) {
			b.append(", redundantPicCntPresent");
		}
		if (picScalingMatrixPresent) {
			b.append(" picScalingMatrix=");
			picScalingMatrix().toString(b);
			b.append(" secondChromaQpIndexOffset=").append(secondChromaQpIndexOffset());
		}
		b.append("]");
		return b.toString();
	}

	public int picParameterSetId() {
		return picParameterSetId;
	}

	public int seqParameterSetId() {
		return seqParameterSetId;
	}

	public EntropyCodingMode entropyCodingMode() {
		return entropyCodingMode;
	}

	public boolean picOrderPresent() {
		return picOrderPresent;
	}

	public int numSliceGroups() {
		return numSliceGroupsMinus1 + 1;
	}

	private void assertMultiSliceGroups() {
		if (numSliceGroupsMinus1 == 0) {
			throw new IllegalStateException("Only valid when miltiple slice groups present");
		}
	}
	public SliceGroupMapType sliceGroupMapType() {
		assertMultiSliceGroups();
		return sliceGroupMapType;
	}

	public int[] runLengthMinus1() {
		assertMultiSliceGroups();
		assertSliceGroupMapType(SliceGroupMapType.INTERLEAVED);
		return runLengthMinus1;
	}

	private void assertSliceGroupMapType(SliceGroupMapType type) {
		if (sliceGroupMapType != type) {
			throw new IllegalStateException("Only valid when sliceGroupMapType is "+type);
		}
	}

	public int[] topLeft() {
		assertMultiSliceGroups();
		assertSliceGroupMapType(SliceGroupMapType.FOREGROUND_AND_LEFTOVER);
		return topLeft;
	}

	public int[] bottomRight() {
		assertMultiSliceGroups();
		assertSliceGroupMapType(SliceGroupMapType.FOREGROUND_AND_LEFTOVER);
		return bottomRight;
	}

	private void assertSliceGroupMapTypeChanging() {
		if (sliceGroupMapType != SliceGroupMapType.CHANGING3 && sliceGroupMapType != SliceGroupMapType.CHANGING4 && sliceGroupMapType != SliceGroupMapType.CHANGING5) {
			throw new IllegalStateException("Only valid when sliceGroupMapType is 'changing' 3, 4 or 5");
		}
	}

	public boolean sliceGroupChangeDirection() {
		assertMultiSliceGroups();
		assertSliceGroupMapTypeChanging();
		return sliceGroupChangeDirection;
	}

	public int sliceGroupChangeRate() {
		assertMultiSliceGroups();
		assertSliceGroupMapTypeChanging();
		return sliceGroupChangeRateMinus1 + 1;
	}

	public int picSizeInMapUnits() {
		assertMultiSliceGroups();
		return picSizeInMapUnitsMinus1 + 1;
	}

	public int[] sliceGroupId() {
		assertMultiSliceGroups();
		return sliceGroupId;
	}

	public int numRefIdxL0Active() {
		return numRefIdxL0ActiveMinus1 + 1;
	}

	public int numRefIdxL1Active() {
		return numRefIdxL1ActiveMinus1 + 1;
	}

	public boolean weightedPred() {
		return weightedPred;
	}

	public WeightedBipred weightedBipred() {
		return weightedBipred;
	}

	public int picInitQp() {
		return picInitQpMinus26 + 26;
	}

	public int picInitQs() {
		return picInitQsMinus26 + 26;
	}

	public int chromaQpIndexOffset() {
		return chromaQpIndexOffset;
	}

	public boolean deblockingFilterControlPresent() {
		return deblockingFilterControlPresent;
	}

	public boolean constrainedIntraPred() {
		return constrainedIntraPred;
	}

	public boolean redundantPicCntPresent() {
		return redundantPicCntPresent;
	}

	public PicScalingMatrix picScalingMatrix() {
		return picScalingMatrix;
	}

	public int secondChromaQpIndexOffset() {
		return secondChromaQpIndexOffset;
	}
}
