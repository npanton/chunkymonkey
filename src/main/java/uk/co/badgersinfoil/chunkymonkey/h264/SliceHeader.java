package uk.co.badgersinfoil.chunkymonkey.h264;

public class SliceHeader {

	private int firstMbInSlice;
	private SliceType sliceType;
	private int picParameterSetId;

	public SliceHeader(H264BitBuf bits) {
		firstMbInSlice = bits.readUE();
		sliceType = SliceType.fromValue(bits.readUE());
		picParameterSetId = bits.readUE();
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
}
