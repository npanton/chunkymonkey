package uk.co.badgersinfoil.chunkymonkey.aac;

import uk.co.badgersinfoil.chunkymonkey.aac.ChannelPairElementAacBlock;
import uk.co.badgersinfoil.chunkymonkey.aac.ChannelPairElementAacBlock.WindowSequence;

public class IndividualChannelStreamHelper {
	private int numWindows;
	private int numWindowGroups;
	private int[] windowGroupLength;
	private int numSwb;
	private int[][] sectSfbOffset;
	private int[] swbOffset;
	private int maxSfb;
	private WindowSequence windowSequence;

	private static boolean bitSet(int value, int bit) {
		return 0 != (value & (1 << bit));
	}

	public IndividualChannelStreamHelper(ChannelPairElementAacBlock.WindowSequence windowSeq,
	                                   Integer scaleFactorGrouping,
	                                   int maxSfb,
	                                   SamplingFrequencyIndex samplingFrequencyIndex)
	{
		this.windowSequence = windowSeq;
		this.maxSfb = maxSfb;
		// Transcription of pseudocode from ISO/IEC 13818-7, Section 8.3.4 (Scalefactor Bands and Grouping)
		switch (windowSeq) {
			case ONLY_LONG:
			case LONG_START:
			case LONG_STOP:
				numWindows = 1;
				numWindowGroups = 1;
				windowGroupLength = new int[1];
				windowGroupLength[numWindowGroups - 1] = 1;
				numSwb = ScaleFactorBands.getNumSwbLongWindow(samplingFrequencyIndex);
				sectSfbOffset = new int[][] { new int[maxSfb+1] }; 
				swbOffset = new int[maxSfb+1];
				for (int i=0; i<=maxSfb; i++) {
					sectSfbOffset[0][i] = ScaleFactorBands.getSwbOffsetLongWindow(samplingFrequencyIndex, i);
					swbOffset[i] = ScaleFactorBands.getSwbOffsetLongWindow(samplingFrequencyIndex, i);
				}
				break;
			case EIGHT_SHORT_SEQUENCE:
				numWindows = 8;
				numWindowGroups = 1;
				windowGroupLength = new int[numWindows];
				windowGroupLength[numWindowGroups-1] = 1;
				numSwb = ScaleFactorBands.getNumSwbShortWindow(samplingFrequencyIndex);
				swbOffset = new int[numSwb+1];
				for (int i=0; i<=numSwb; i++) {
					swbOffset[i] = ScaleFactorBands.getSwbOffsetShortWindow(samplingFrequencyIndex, i);
				}
				for (int i=0; i<numWindows-1; i++) {
					if (!bitSet(scaleFactorGrouping, 6-i)) {
						numWindowGroups += 1;
						windowGroupLength[numWindowGroups-1] = 1;
					} else {
						windowGroupLength[numWindowGroups-1] += 1;
					}
				}
				sectSfbOffset = new int[numWindowGroups][]; 
				for (int g = 0; g < numWindowGroups; g++) {
					int sectSfb = 0;
					int offset = 0;
					sectSfbOffset[g] = new int[maxSfb]; 
					for (int i = 0; i < maxSfb; i++) {
						int width = ScaleFactorBands.getSwbOffsetShortWindow(samplingFrequencyIndex, i+1) - ScaleFactorBands.getSwbOffsetShortWindow(samplingFrequencyIndex, i);
						width *= windowGroupLength[g];
						sectSfbOffset[g][sectSfb++] = offset;
						offset += width;
					}
					sectSfbOffset[g][sectSfb] = offset;
				}
				break;
		}
	}

	public int getNumWindowGroups() {
		return numWindowGroups;
	}

	public int getMaxSfb() {
		return maxSfb;
	}

	public WindowSequence getWindowSequence() {
		return windowSequence;
	}
}
	