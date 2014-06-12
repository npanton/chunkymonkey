package uk.co.badgersinfoil.chunkymonkey.h264;

import java.util.Arrays;

public class ScalingList {
	private int[] scalingList;
	private boolean useDefaultScalingMatrixFlag;

	public ScalingList(H264BitBuf bits, int size) {
	        scalingList = new int[size];
	        int lastScale = 8;
	        int nextScale = 8;
	        for (int j = 0; j < size; j++) {
	            if (nextScale != 0) {
	                int deltaScale = bits.readSE();
	                nextScale = (lastScale + deltaScale + 256) % 256;
	                useDefaultScalingMatrixFlag = j == 0 && nextScale == 0;
	            }
	            scalingList[j] = nextScale == 0 ? lastScale : nextScale;
	            lastScale = scalingList[j];
	        }
	}
	@Override
	public String toString() {
		return Arrays.toString(scalingList);
	}
}