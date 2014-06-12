package uk.co.badgersinfoil.chunkymonkey.h264;

import java.util.Arrays;
import io.netty.buffer.ByteBuf;

public class SeqParamSet {
	public static enum ChromaFormat {
		MONOCHROME,
		YUV420,
		YUV422,
		YUV444;

		public static ChromaFormat forIndex(int i) {
			switch (i) {
			case 0: return MONOCHROME;
			case 1: return YUV420;
			case 2: return YUV422;
			case 3: return YUV444;
			default: throw new IllegalArgumentException("Valid indexes are in the range 0-3: "+i);
			}
		}
	}
	public static class ScalingMatrix {
		private ScalingList[] scalingList4x4 = new ScalingList[8];
		private ScalingList[] scalingList8x8 = new ScalingList[8];

		public ScalingMatrix(H264BitBuf bits, int chromaFormatIdc) {
			final int count = (chromaFormatIdc != 3) ? 8 : 12;
			for (int i = 0; i < count; i++) {
				boolean seqScalingListPresentFlag = bits.readBool();
				if (seqScalingListPresentFlag) {
			                if (i < 6) {
			                        scalingList4x4[i] = new ScalingList(bits, 16);
			                    } else {
			                        scalingList8x8[i - 6] = new ScalingList(bits, 64);
			                    }
				}
			}
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
	public static class HrdParameters {

		private int cpbCntMinus1;
		private int bitRateScale;
		private int cpbSizeScale;
		private int[] bitRateValueMinus1;
		private int[] cpbSizeValueMinus1;
		private boolean[] cbrFlag;
		private int initialCpbRemovalDelayLengthMinus1;
		private int cpbRemovalDelayLengthMinus1;
		private int dpbOutputDelayLengthMinus1;
		private int timeOffsetLength;

		public HrdParameters(H264BitBuf bits) {
			cpbCntMinus1 = bits.readUE();
			bitRateScale = bits.readBits(4);
			cpbSizeScale = bits.readBits(4);
			bitRateValueMinus1 = new int[cpbCntMinus1+1];
			cpbSizeValueMinus1 = new int[cpbCntMinus1+1];
			cbrFlag = new boolean[cpbCntMinus1+1];
			for(int schedSelIdx=0; schedSelIdx<=cpbCntMinus1; schedSelIdx++) {
				bitRateValueMinus1[schedSelIdx] = bits.readUE();
				cpbSizeValueMinus1[schedSelIdx] = bits.readUE();
				cbrFlag[schedSelIdx] = bits.readBool();
			}
			initialCpbRemovalDelayLengthMinus1 = bits.readBits(5);
			cpbRemovalDelayLengthMinus1 = bits.readBits(5);
			dpbOutputDelayLengthMinus1 = bits.readBits(5);
			timeOffsetLength = bits.readBits(5);
		}

		public int cpbCntMinus1() {
			return cpbCntMinus1;
		}
		public int bitRateScale() {
			return bitRateScale;
		}
		public int cpbSizeScale() {
			return cpbSizeScale;
		}
		public int cpbRemovalDelayLengthMinus1() {
			return cpbRemovalDelayLengthMinus1;
		}
		public int dpbOutputDelayLengthMinus1() {
			return dpbOutputDelayLengthMinus1;
		}
		public int timeOffsetLength() {
			return timeOffsetLength;
		}

		public StringBuilder toString(StringBuilder b) {
			return b.append(" cpbCntMinus1=").append(cpbCntMinus1())
			        .append(" bitRateScale=").append(bitRateScale())
			        .append(" cpbSizeScale=").append(cpbSizeScale());
		}
	}
	public static class TimingInfo {

		private long numUnitsInTick;
		private long timeScale;
		private boolean fixedFrameRateFlag;

		public TimingInfo(H264BitBuf bits) {
			numUnitsInTick = bits.readBits(8) << 24 | bits.readBits(8) << 16 | bits.readBits(8) << 8 | bits.readBits(8);
			timeScale = bits.readBits(8) << 24 | bits.readBits(8) << 16 | bits.readBits(8) << 8 | bits.readBits(8);
			fixedFrameRateFlag = bits.readBool();
		}

		public long numUnitsInTick() {
			return numUnitsInTick;
		}
		public long timeScale() {
			return timeScale;
		}
		public boolean fixedFrameRateFlag() {
			return fixedFrameRateFlag;
		}

		public StringBuilder toString(StringBuilder b) {
			return b.append(" numUnitsInTick=").append(numUnitsInTick())
			        .append(" timeScale=").append(timeScale())
			        .append(" fixedFrameRateFlag=").append(fixedFrameRateFlag());
		}
	}
	public static class ColourDescription {

		private int colourPrimaries;
		private int transferCharacteristics;
		private int matrixCoefficients;

		public ColourDescription(H264BitBuf bits) {
			colourPrimaries = bits.readBits(8);
			transferCharacteristics = bits.readBits(8);
			matrixCoefficients = bits.readBits(8);
		}

		public int colourPrimaries() {
			return colourPrimaries;
		}

		public int transferCharacteristics() {
			return transferCharacteristics;
		}

		public int matrixCoefficients() {
			return matrixCoefficients;
		}

		public StringBuilder toString(StringBuilder b) {
			return b.append(" colourPrimaries=").append(colourPrimaries())
			        .append(" transferCharacteristics=").append(transferCharacteristics())
			        .append(" matrixCoefficients=").append(matrixCoefficients());
		}
	}
	public static class VideoSignalType {

		private int videoFormat;
		private boolean videoFullRangeFlag;
		private ColourDescription colourDescription;

		public VideoSignalType(H264BitBuf bits) {
			videoFormat = bits.readBits(3);
			videoFullRangeFlag = bits.readBool();
			boolean colourDescriptionPresentFlag = bits.readBool();
			if (colourDescriptionPresentFlag) {
				colourDescription = new ColourDescription(bits);
			}
		}

		public StringBuilder toString(StringBuilder b) {
			return b.append(" videoFormat=").append(videoFormat())
			        .append(" videoFullRangeFlag=").append(videoFullRangeFlag());
		}
		public int videoFormat() {
			return videoFormat;
		}
		public boolean videoFullRangeFlag() {
			return videoFullRangeFlag;
		}
	}
	public static class AspectRatioInfo {

		private int aspectRatioIdc;
		private int width;
		private int height;

		public AspectRatioInfo(H264BitBuf bits) {
			aspectRatioIdc = bits.readBits(8);
			switch (aspectRatioIdc) {
			case 1: width=1; height=1; break;
			case 2: width=12; height=11; break;
			case 3: width=10; height=11; break;
			case 4: width=16; height=11; break;
			case 5: width=40; height=33; break;
			case 6: width=24; height=11; break;
			case 7: width=20; height=11; break;
			case 8: width=32; height=11; break;
			case 9: width=80; height=33; break;
			case 10: width=18; height=11; break;
			case 11: width=15; height=11; break;
			case 12: width=64; height=33; break;
			case 13: width=160; height=99; break;
			case 255:
				width = bits.readBits(8) << 8 | bits.readBits(8);
				height = bits.readBits(8) << 8 | bits.readBits(8);
				break;
			}
		}

		public StringBuilder toString(StringBuilder b) {
			b.append(" aspectRatioIdc=").append(aspectRatioIdc());
			if (aspectRatioIdc() == 0) {
				b.append(" <unspecified aspect ratio>");
			} else if (aspectRatioIdc() >= 14 && aspectRatioIdc() <= 254) {
				b.append(" <reserved aspect ratio indicator value>");
			} else {
				b.append(" ratio=").append(width()).append(":").append(height());
			}
			return b;
		}
		@Override
		public String toString() {
			return toString(new StringBuilder()).toString();
		}
		public int aspectRatioIdc() {
			return aspectRatioIdc;
		}
		public int width() {
			return width;
		}
		public int height() {
			return height;
		}
	}
	public static enum OverscanInfo {
		UNSPECIFIED, APPROPRIATE, INAPPROPRIATE;
	}
	public static class VuiParameters {

		private AspectRatioInfo aspectRatioInfo;
		private OverscanInfo overscanInfo;
		private VideoSignalType videoSignalType;
		private int chromaSampleLocTypeTopField = 0;
		private int chromaSampleLocTypeBottomField = 0;
		private TimingInfo timingInfo;
		private HrdParameters nalHrdParameters;
		private HrdParameters vclHrdParameters;
		private boolean lowDelayHrdFlag;
		private boolean picStructPresentFlag;

		public VuiParameters(H264BitBuf bits) {
			boolean aspectRatioInfoPresentFlag = bits.readBool();
			if (aspectRatioInfoPresentFlag) {
				aspectRatioInfo = new AspectRatioInfo(bits);
			}
			boolean overscanInfoPresentFlag = bits.readBool();
			if (overscanInfoPresentFlag) {
				boolean overscanAppropriateFlag = bits.readBool();
				if (overscanAppropriateFlag) {
					overscanInfo = OverscanInfo.APPROPRIATE;
				} else {
					overscanInfo = OverscanInfo.INAPPROPRIATE;
				}
			} else {
				overscanInfo = OverscanInfo.UNSPECIFIED;
			}
			boolean videoSignalTypePresentFlag = bits.readBool();
			if (videoSignalTypePresentFlag) {
				videoSignalType = new VideoSignalType(bits);
			}
			boolean chromaLocInfoPresentFlag = bits.readBool();
			if (chromaLocInfoPresentFlag) {
				chromaSampleLocTypeTopField  = bits.readUE();
				chromaSampleLocTypeBottomField = bits.readUE();
			}
			boolean timingInfoPresentFlag = bits.readBool();
			if (timingInfoPresentFlag) {
				timingInfo = new TimingInfo(bits);
			}
			boolean nalHrdParametersPresentFlag = bits.readBool();
			if (nalHrdParametersPresentFlag) {
				nalHrdParameters = new HrdParameters(bits);
			}
			boolean vclHrdParametersPresentFlag = bits.readBool();
			if (vclHrdParametersPresentFlag) {
				vclHrdParameters = new HrdParameters(bits);
			}
			if (nalHrdParametersPresentFlag || vclHrdParametersPresentFlag) {
				lowDelayHrdFlag = bits.readBool();
			}
			picStructPresentFlag = bits.readBool();
		}

		public StringBuilder toString(StringBuilder b) {
			if (aspectRatioInfo != null) {
				aspectRatioInfo.toString(b);
			}
			b.append(" overscanInfo=").append(overscanInfo());
			if (videoSignalType != null) {
				videoSignalType.toString(b);
			}
			b.append(" chromaSampleLocTypeTopField=").append(chromaSampleLocTypeTopField())
			 .append(" chromaSampleLocTypeBottomField=").append(chromaSampleLocTypeBottomField());
			if (timingInfo != null) {
				timingInfo.toString(b);
			}
			if (nalHrdParameters!=null) {
				b.append(" nalHrdParameters={");
				nalHrdParameters.toString(b);
				b.append(" }");
			}
			if (vclHrdParameters!=null) {
				b.append(" vclHrdParameters={");
				vclHrdParameters.toString(b);
				b.append(" }");
			}
			if (nalHrdParameters!=null || vclHrdParameters!=null) {
				b.append(" lowDelayHrdFlag=").append(lowDelayHrdFlag());
			}
			b.append(" picStructPresentFlag=").append(picStructPresentFlag());
			return b;
		}
		public OverscanInfo overscanInfo() {
			return overscanInfo;
		}
		public int chromaSampleLocTypeTopField() {
			return chromaSampleLocTypeTopField;
		}
		public int chromaSampleLocTypeBottomField() {
			return chromaSampleLocTypeBottomField;
		}
		public TimingInfo timingInfo() {
			return timingInfo;
		}
		public boolean lowDelayHrdFlag() {
			return lowDelayHrdFlag;
		}
		public boolean picStructPresentFlag() {
			return picStructPresentFlag;
		}
		@Override
		public String toString() {
			return toString(new StringBuilder()).toString();
		}
		public HrdParameters nalHrdParameters() {
			return nalHrdParameters;
		}
		public HrdParameters vclHrdParameters() {
			return vclHrdParameters;
		}
	}
	public static class FrameCrop {
		private int leftOffset;
		private int rightOffset;
		private int topOffset;
		private int bottomOffset;
		public FrameCrop(int leftOffset, int rightOffset,
				int topOffset, int bottomOffset) {
			this.leftOffset = leftOffset;
			this.rightOffset = rightOffset;
			this.topOffset = topOffset;
			this.bottomOffset = bottomOffset;
		}
		public int leftOffset() {
			return leftOffset;
		}
		public int rightOffset() {
			return rightOffset;
		}
		public int topOffset() {
			return topOffset;
		}
		public int bottomOffset() {
			return bottomOffset;
		}
	}

	private int profileIdc;
	private boolean constraintSet0Flag;
	private boolean constraintSet1Flag;
	private boolean constraintSet2Flag;
	private boolean constraintSet3Flag;
	private int levelIdc;
	private int seqParamSetId;

	// the following group of properties only valid when isProfileWithExtraInfo()==true
	private ChromaFormat chromaFormat;
	private boolean separateColourPlaneFlag = false;
	private int bitDepthLumaMinus8;
	private int bitDepthChromaMinus8;
	private boolean qpprimeYZeroTransformBypassFlag;
	private ScalingMatrix scalingMatrix = null;

	private int log2MaxFrameNumMinus4;
	private int picOrderCntType;
	private int log2MaxPicOrderCntLsbMinus4;
	private boolean deltaPicOrderAlwaysZeroFlag;
	private int offsetForNonRefPic;
	private int offsetForTopToBottomField;
	private int[] offsetForRefFrame;
	private int numRefFrames;
	private boolean gapsInFrameNumValueAllowedFlag;
	private int picWidthInMbsMinus1;
	private int picHeightInMapUnitsMinus1;
	private boolean frameMbsOnlyFlag;
	private boolean mbAdaptiveFrameFieldFlag = false;
	private boolean direct8x8InferenceFlag;
	private FrameCrop frameCrop;
	private VuiParameters vuiParameters;

	public SeqParamSet(ByteBuf buf) {
		H264BitBuf bits = new H264BitBuf(buf);
		profileIdc = bits.readBits(8);
		constraintSet0Flag = bits.readBool();
		constraintSet1Flag = bits.readBool();
		constraintSet2Flag = bits.readBool();
		constraintSet3Flag = bits.readBool();
		int reserved_zero_4bits = bits.readBits(4);
		levelIdc = bits.readBits(8);
		seqParamSetId = bits.readUE();
		if (isProfileWithExtraInfo(profileIdc)) {
			int chromaFormatIdc = bits.readUE();
			// TODO: report values outside allowed range (rather than throwing),
			chromaFormat = ChromaFormat.forIndex(chromaFormatIdc);
			if (chromaFormatIdc == 3) {
				separateColourPlaneFlag = bits.readBool();
			}
			bitDepthLumaMinus8 = bits.readUE();
			bitDepthChromaMinus8 = bits.readUE();
			qpprimeYZeroTransformBypassFlag = bits.readBool();
			boolean seqScalingMatrixPresent = bits.readBool();
			if (seqScalingMatrixPresent) {
				scalingMatrix = new ScalingMatrix(bits, chromaFormatIdc);
			}
		} else {
			chromaFormat = ChromaFormat.YUV420;
		}
		log2MaxFrameNumMinus4 = bits.readUE();
		picOrderCntType = bits.readUE();
		switch (picOrderCntType) {
		case 0:
			log2MaxPicOrderCntLsbMinus4  = bits.readUE();
			// TODO: check range is 0-12
			break;
		case 1:
			deltaPicOrderAlwaysZeroFlag = bits.readBool();
			offsetForNonRefPic = bits.readSE();
			offsetForTopToBottomField = bits.readSE();
			int numRefFramesInPicOrderCntCycle = bits.readUE();
			offsetForRefFrame = new int[numRefFramesInPicOrderCntCycle];
			for (int i=0; i<numRefFramesInPicOrderCntCycle; i++) {
				offsetForRefFrame[i] = bits.readSE();
			}
			break;
		}
		numRefFrames = bits.readUE();
		gapsInFrameNumValueAllowedFlag = bits.readBool();
		picWidthInMbsMinus1 = bits.readUE();
		picHeightInMapUnitsMinus1 = bits.readUE();
		frameMbsOnlyFlag = bits.readBool();
		if (!frameMbsOnlyFlag) {
			mbAdaptiveFrameFieldFlag = bits.readBool();
		}
		direct8x8InferenceFlag = bits.readBool();
		boolean frameCroppingFlag = bits.readBool();
		if (frameCroppingFlag) {
			frameCrop = new FrameCrop(bits.readUE(), bits.readUE(),
			                          bits.readUE(), bits.readUE());
		}
		boolean vuiParametersPresentFlag = bits.readBool();
		if (vuiParametersPresentFlag) {
			vuiParameters = new VuiParameters(bits);
		}
	}

	private static boolean isProfileWithExtraInfo(int profileIdc) {
		return profileIdc == 100 || profileIdc == 110
		   || profileIdc == 122 || profileIdc == 244
		   || profileIdc == 44 || profileIdc == 83
		   || profileIdc == 86;
	}

	private static void assertProfileWithExtraInfo(int profileIdc) {
		if (!isProfileWithExtraInfo(profileIdc)) {
			throw new IllegalStateException("property not available for profile "+profileIdc);
		}
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("profileIdc=").append(profileIdc())
		 .append(" constraintSet0Flag=").append(constraintSet0Flag())
		 .append(" constraintSet1Flag=").append(constraintSet1Flag())
		 .append(" constraintSet2Flag=").append(constraintSet2Flag())
		 .append(" levelIdc=").append(levelIdc())
		 .append(" seqParamSetId=").append(seqParamSetId());
		if (isProfileWithExtraInfo(profileIdc)) {
			b.append(" chromaFormat=").append(chromaFormat());
			if (chromaFormat() == ChromaFormat.YUV444) {
				b.append("separateColourPlaneFlag="+separateColourPlaneFlag());
			}
			b.append(" bitDepthLumaMinus8=").append(bitDepthLumaMinus8())
			 .append(" bitDepthChromaMinus8=").append(bitDepthChromaMinus8())
			 .append(" qpprimeYZeroTransformBypassFlag=").append(qpprimeYZeroTransformBypassFlag());
			if (scalingMatrix() != null) {
				b.append(" scalingMatrix=");
				scalingMatrix.toString(b);
			}
		}
		b.append(" log2MaxFrameNumMinus4=").append(log2MaxFrameNumMinus4())
		 .append(" picOrderCntType=").append(picOrderCntType());
		switch (picOrderCntType) {
		case 0:
			b.append(" log2MaxPicOrderCntLsbMinus4=").append(log2MaxPicOrderCntLsbMinus4());
			break;
		case 1:
			b.append(" deltaPicOrderAlwaysZeroFlag=").append(deltaPicOrderAlwaysZeroFlag())
			 .append(" offsetForNonRefPic=").append(offsetForNonRefPic())
			 .append(" offsetForTopToBottomField=").append(offsetForTopToBottomField())
			 .append(" offsetForRefFrame=").append(Arrays.toString(offsetForRefFrame));
			break;
		}
		b.append(" numRefFrames=").append(numRefFrames())
		 .append(" gapsInFrameNumValueAllowedFlag=").append(gapsInFrameNumValueAllowedFlag())
		 .append(" picWidthInMbsMinus1=").append(picWidthInMbsMinus1())
		 .append(" picHeightInMapUnitsMinus1=").append(picHeightInMapUnitsMinus1())
		 .append(" frameMbsOnlyFlag=").append(frameMbsOnlyFlag());
		if (!frameMbsOnlyFlag()) {
			b.append(" mbAdaptiveFrameFieldFlag=").append(mbAdaptiveFrameFieldFlag());
		}
		b.append(" direct8x8InferenceFlag=").append(direct8x8InferenceFlag());
		if (frameCrop != null) {
			b.append(" frameCrop={ leftOffset=").append(frameCrop.leftOffset())
			 .append(" rightOffset=").append(frameCrop.rightOffset())
			 .append(" topOffset=").append(frameCrop.topOffset())
			 .append(" bottomOffset=").append(frameCrop.bottomOffset())
			 .append(" }");
		}
		if (vuiParameters != null) {
			b.append(" vuiParameters={ ");
			vuiParameters.toString(b);
			b.append(" }");
		}
		return b.toString();
	}

	public int profileIdc() {
		return profileIdc;
	}
	public boolean constraintSet0Flag() {
		return constraintSet0Flag;
	}
	public boolean constraintSet1Flag() {
		return constraintSet1Flag;
	}
	public boolean constraintSet2Flag() {
		return constraintSet2Flag;
	}
	public int levelIdc() {
		return levelIdc;
	}
	public int seqParamSetId() {
		return seqParamSetId;
	}
	public ChromaFormat chromaFormat() {
		return chromaFormat;
	}
	public boolean separateColourPlaneFlag() {
		return separateColourPlaneFlag;
	}
	public int bitDepthLumaMinus8() {
		assertProfileWithExtraInfo(profileIdc);
		return bitDepthLumaMinus8;
	}
	public int bitDepthChromaMinus8() {
		assertProfileWithExtraInfo(profileIdc);
		return bitDepthChromaMinus8;
	}
	public boolean qpprimeYZeroTransformBypassFlag() {
		assertProfileWithExtraInfo(profileIdc);
		return qpprimeYZeroTransformBypassFlag;
	}
	public ScalingMatrix scalingMatrix() {
		assertProfileWithExtraInfo(profileIdc);
		return scalingMatrix;
	}

	public int log2MaxFrameNumMinus4() {
		return log2MaxFrameNumMinus4;
	}
	public int picOrderCntType() {
		return picOrderCntType;
	}
	public int log2MaxPicOrderCntLsbMinus4() {
		if (picOrderCntType != 0) {
			throw new IllegalStateException("log2MaxPicOrderCntLsbMinus4 not valid unless picOrderCntType=0");
		}
		return log2MaxPicOrderCntLsbMinus4;
	}
	public boolean deltaPicOrderAlwaysZeroFlag() {
		if (picOrderCntType != 1) {
			throw new IllegalStateException("deltaPicOrderAlwaysZeroFlag not valid unless picOrderCntType=1");
		}
		return deltaPicOrderAlwaysZeroFlag;
	}
	public int offsetForNonRefPic() {
		if (picOrderCntType != 1) {
			throw new IllegalStateException("deltaPicOrderAlwaysZeroFlag not valid unless picOrderCntType=1");
		}
		return offsetForNonRefPic;
	}
	public int offsetForTopToBottomField() {
		if (picOrderCntType != 1) {
			throw new IllegalStateException("offsetForTopToBottomField not valid unless picOrderCntType=1");
		}
		return offsetForTopToBottomField;
	}
	public int numRefFrames() {
		return numRefFrames;
	}
	public boolean gapsInFrameNumValueAllowedFlag() {
		return gapsInFrameNumValueAllowedFlag;
	}
	public int picWidthInMbsMinus1() {
		return picWidthInMbsMinus1;
	}
	public int picHeightInMapUnitsMinus1() {
		return picHeightInMapUnitsMinus1;
	}
	public boolean frameMbsOnlyFlag() {
		return frameMbsOnlyFlag;
	}
	public boolean mbAdaptiveFrameFieldFlag() {
		return mbAdaptiveFrameFieldFlag;
	}
	public boolean direct8x8InferenceFlag() {
		return direct8x8InferenceFlag;
	}
	public FrameCrop frameCrop() {
		return frameCrop;
	}
	public VuiParameters vuiParameters() {
		return vuiParameters;
	}
}
