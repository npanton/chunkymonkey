package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.event.Alert;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;
import uk.co.badgersinfoil.chunkymonkey.h264.NALUnit.UnitType;
import uk.co.badgersinfoil.chunkymonkey.h264.NalUnitConsumer.NalUnitContext;
import uk.co.badgersinfoil.chunkymonkey.h264.SliceType.SliceName;

/**
 * TODO: currently incomplete vs. the fields present in the spec
 */
public class SliceHeader {

	@LogFormat("Reordering #{reorderingNum}: Bad reordering_of_pic_nums_idc value {reorderingOfPicNumsIdc} (must be 0-3)")
	public static class BadReorderingOfPicNumsEvent extends Alert { }

	public static class RefPicListReordering {

		private RefPicListReordering() { }

		public static RefPicListReordering parse(NalUnitContext ctx,
		                                         SliceHeader h,
		                                         H264BitBuf bits,
		                                         Reporter rep)
		{
			RefPicListReordering  result = new RefPicListReordering();
			if (result.parse0(ctx, h, bits, rep)) {
				return result;
			}
			return null;
		}

		private boolean parse0(NalUnitContext ctx,
		                       SliceHeader h,
		                       H264BitBuf bits,
		                       Reporter rep)
		{
			if (h.sliceType.sliceName() != SliceName.I && h.sliceType.sliceName() != SliceName.SI) {
				boolean refPicListReorderingFlagL0 = bits.readBool();
				if (refPicListReorderingFlagL0) {
					int reordering_of_pic_nums_idc;
					int count = 0;
					do {
						reordering_of_pic_nums_idc = bits.readUE();
						if (reordering_of_pic_nums_idc > 3) {
							new BadReorderingOfPicNumsEvent()
								.with("reorderingNum", count)
								.with("reorderingOfPicNumsIdc", reordering_of_pic_nums_idc)
								.at(ctx)
								.to(rep);
							return false;
						}
						if (reordering_of_pic_nums_idc == 0 || reordering_of_pic_nums_idc == 1) {
							int absDiffPicNumMinus1 = bits.readUE();
						} else if (reordering_of_pic_nums_idc == 2) {
							int longTermPicNum = bits.readUE();
						}
						count++;
					} while (reordering_of_pic_nums_idc != 3);
				}
			}
			if (h.sliceType.sliceName() == SliceName.B) {
				boolean refPicListReorderingFlagL1 = bits.readBool();
				if (refPicListReorderingFlagL1) {
					int reordering_of_pic_nums_idc;
					int count = 0;
					do {
						reordering_of_pic_nums_idc = bits.readUE();
						if (reordering_of_pic_nums_idc > 3) {
							new BadReorderingOfPicNumsEvent()
								.with("reorderingNum", count)
								.with("reorderingOfPicNumsIdc", reordering_of_pic_nums_idc)
								.at(ctx)
								.to(rep);
							return false;
						}
						if (reordering_of_pic_nums_idc == 0 || reordering_of_pic_nums_idc == 1) {
							int absDiffPicNumMinus1 = bits.readUE();
						} else if (reordering_of_pic_nums_idc == 2) {
							int longTermPicNum = bits.readUE();
						}
						count++;
					} while (reordering_of_pic_nums_idc != 3);
				}
			}
			return true;
		}
	}

	private int firstMbInSlice;
	private SliceType sliceType;
	private int picParameterSetId;
	private Integer colorPlaneId = null;
	private int frameNum;
	private Boolean fieldPicFlag = null;
	private Boolean bottomFieldFlag = null;
	private Integer idrPicId = null;
	private Integer picOrderCntLsb = null;
	private Integer deltaPicOrderCntBottom = null;
	private Integer deltaPicOrderCnt0 = null;
	private Integer deltaPicOrderCnt1 = null;
	private Integer redundantPicCnt = null;
	private Boolean directSpatialMvPred = null;
	private Boolean numRefIdxActiveOverride = null;
	private Integer numRefIdxL0ActiveMinus1 = null;
	private Integer numRefIdxL1ActiveMinus1 = null;
	private RefPicListReordering refPicListReordering;

	private SliceHeader() {
	}

	public static SliceHeader parse(NalUnitContext ctx,
	                                H264BitBuf bits,
	                                Reporter rep)
	{
		SliceHeader result = new SliceHeader();
		if (result.parse0(ctx, bits, rep)) {
			return result;
		}
		return null;
	}

	private boolean parse0(NalUnitContext ctx,
	                       H264BitBuf bits,
	                       Reporter rep)
	{
		firstMbInSlice = bits.readUE();
		sliceType = SliceType.fromValue(bits.readUE());
		picParameterSetId = bits.readUE();
		// TODO: derive via picParameterSetId
		H264Context hctx = ctx.getH264Context();
		SeqParamSet seqParamSet = hctx.lastSeqParamSet();
		if (seqParamSet.separateColourPlaneFlag()) {
			colorPlaneId = bits.readBits(2);
		}
		frameNum = bits.readBits(seqParamSet.log2MaxFrameNumMinus4()+4);
		if (!seqParamSet.frameMbsOnlyFlag()) {
			fieldPicFlag = bits.readBool();
			if (fieldPicFlag) {
				bottomFieldFlag = bits.readBool();
			}
		}
		if (hctx.getNalUnit().nalUnitType() == UnitType.SLICE_LAYER_WITHOUT_PARTITIONING_IDR) {
			idrPicId  = bits.readUE();
		}
		PicParamSet picParamSet = hctx.lastPicParamSet();
		if (seqParamSet.picOrderCntType() == 0) {
			picOrderCntLsb = bits.readBits(seqParamSet.log2MaxPicOrderCntLsbMinus4()+4);
			if (picParamSet.picOrderPresent() && !fieldPicFlag()) {
				deltaPicOrderCntBottom  = bits.readSE();
			}
		}
		if (seqParamSet.picOrderCntType() == 1 && !seqParamSet.deltaPicOrderAlwaysZeroFlag()) {
			deltaPicOrderCnt0 = bits.readSE();
			if (picParamSet.picOrderPresent() && !fieldPicFlag()) {
				deltaPicOrderCnt1 = bits.readSE();
			}
		}
		if (picParamSet.redundantPicCntPresent()) {
			redundantPicCnt  = bits.readUE();
		}
		if (sliceType.sliceName() == SliceType.SliceName.B) {
			directSpatialMvPred = bits.readBool();
		}
		if (sliceType.sliceName() == SliceType.SliceName.P || sliceType.sliceName() == SliceType.SliceName.SP  || sliceType.sliceName() == SliceType.SliceName.B) {
			numRefIdxActiveOverride = bits.readBool();
			if (numRefIdxActiveOverride) {
				numRefIdxL0ActiveMinus1 = bits.readUE();
				if (sliceType.sliceName() == SliceType.SliceName.B) {
					numRefIdxL1ActiveMinus1 = bits.readUE();
				}
			}
		}
		refPicListReordering = RefPicListReordering.parse(ctx, this, bits, rep);
		// TODO: pred_weight_table() onwards...
		return true;
	}

	public int getFirstMbInSlice() {
		return firstMbInSlice;
	}
	public SliceType getSliceType() {
		return sliceType;
	}
	public int getPicParameterSetId() {
		return picParameterSetId;
	}

	public boolean fieldPicFlag() {
		return fieldPicFlag==null ? false : fieldPicFlag;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("firstMbInSlice=").append(firstMbInSlice)
		 .append(" sliceType=").append(sliceType)
		 .append(" picParameterSetId=").append(picParameterSetId);
		if (colorPlaneId != null) {
			b.append(" colorPlaneId=").append(colorPlaneId);
		}
		b.append(" frameNum=").append(frameNum);
		if (fieldPicFlag != null) {
			b.append(" fieldPicFlag=").append(fieldPicFlag);
			if (fieldPicFlag) {
				b.append(" bottomFieldFlag=").append(bottomFieldFlag);
			}
		}
		if (idrPicId != null) {
			b.append(" idrPicId=").append(idrPicId);
		}
		if (picOrderCntLsb != null) {
			b.append(" picOrderCntLsb=").append(picOrderCntLsb);
		}
		if (deltaPicOrderCntBottom != null) {
			b.append(" deltaPicOrderCntBottom=").append(deltaPicOrderCntBottom);
		}
		if (deltaPicOrderCnt0 != null) {
			b.append(" deltaPicOrderCnt0=").append(deltaPicOrderCnt0);
		}
		if (deltaPicOrderCnt1 != null) {
			b.append(" deltaPicOrderCnt1=").append(deltaPicOrderCnt1);
		}
		if (redundantPicCnt != null) {
			b.append(" redundantPicCnt=").append(redundantPicCnt);
		}
		if (directSpatialMvPred != null) {
			b.append(" directSpatialMvPred=").append(directSpatialMvPred);
		}
		if (numRefIdxActiveOverride != null) {
			b.append(" numRefIdxActiveOverride=").append(numRefIdxActiveOverride);
		}
		if (numRefIdxL0ActiveMinus1 != null) {
			b.append(" numRefIdxL0ActiveMinus1=").append(numRefIdxL0ActiveMinus1);
		}
		if (numRefIdxL1ActiveMinus1 != null) {
			b.append(" numRefIdxL1ActiveMinus1=").append(numRefIdxL1ActiveMinus1);
		}
		return b.toString();
	}
}
