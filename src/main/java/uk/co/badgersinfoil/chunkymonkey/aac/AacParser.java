package uk.co.badgersinfoil.chunkymonkey.aac;

import uk.co.badgersinfoil.chunkymonkey.aac.AacBlock.BlockType;
import uk.co.badgersinfoil.chunkymonkey.aac.ChannelPairElementAacBlock.WindowSequence;
import io.netty.buffer.ByteBuf;

public class AacParser {
	SamplingFrequencyIndex samplingFrequencyIndex;
	
	public AacParser(SamplingFrequencyIndex samplingFrequencyIndex) {
		this.samplingFrequencyIndex = samplingFrequencyIndex;
	}

	public void parser(ByteBuf aacFrame) {
		if (aacFrame.readableBytes() < 8) {
			// TODO: flag problem to caller?
			return;
		}
		BitBuf bits = new BitBuf(aacFrame);
		int typeVal = bits.readBits(3);
		BlockType type = AacBlock.BlockType.valueOf(typeVal);
		System.out.print("AAC Block Type: "+type);
		if (type != AacBlock.BlockType.CPE) {
			return;
		}
		int elementInstanceTag = bits.readBits(4);
		boolean commonWindow = bits.readBits(1) == 1;
		System.out.print(
			" elementInstanceTag="+elementInstanceTag
			+" commonWindow="+commonWindow
		);
		IndividualChannelStreamHelper icsHelper = null;

		if (commonWindow) {
			icsHelper = parseIcsInfo(bits);
			boolean msMaskPresent = bits.readBits(1) == 1;
			if (msMaskPresent) {
				boolean[][] msUsed = new boolean[icsHelper.getNumWindowGroups()][];
				for (int g = 0; g < icsHelper.getNumWindowGroups(); g++) {
					msUsed[g] = new boolean[icsHelper.getMaxSfb()];
					for (int sfb = 0; sfb < icsHelper.getMaxSfb(); sfb++) {
						msUsed[g][sfb] = bits.readBits(1) == 1;
					}
				}
			}
		}

		parseIndividualChannelStream(bits, commonWindow, icsHelper);
		parseIndividualChannelStream(bits, commonWindow, icsHelper);

		System.out.println();
		//switch (type) {
		//case CPE: return new ChannelPairElementAacBlock(aacFrame);
		//default: return null;
		//}
		
	}

	private void parseIndividualChannelStream(BitBuf bits,
	                                          boolean commonWindow, IndividualChannelStreamHelper icsHelper)
	{
		int globalGain = bits.readBits(8);
		System.out.print(" globalGain="+globalGain);
		if (!commonWindow) {
			icsHelper = parseIcsInfo(bits);
		}
		parseSectionData(bits, icsHelper);
/*		parseScaleFactorData(bits);
		
		boolean pulseDataPresent = bits.readBits(1) == 1;
		if (pulseDataPresent) {
			parsePulseData(bits);
		}

		boolean tnsDataPresent = bits.readBits(1) == 1;
		if (tnsDataPresent) {
			parseTnsData(bits);
		}

		boolean gainControlDataPresent = bits.readBits(1) == 1;
		if (gainControlDataPresent) {
			parseGainControlData(bits);
		}
		
		parseSpectralData(bits);
*/
	}

	private void parseSectionData(BitBuf bits, IndividualChannelStreamHelper icsHelper) {
		final int sectLenIncrBits;
		if (icsHelper.getWindowSequence() == WindowSequence.EIGHT_SHORT_SEQUENCE) {
			sectLenIncrBits = 3;
		} else {
			sectLenIncrBits = 5;
		}
		final int sectEscVal = (1<<sectLenIncrBits) - 1;
		int sectCb[][] = new int [icsHelper.getNumWindowGroups()][];
		int sectStart[][] = new int [icsHelper.getNumWindowGroups()][];
		int sectEnd[][] = new int [icsHelper.getNumWindowGroups()][];
		int sfbCb[][] = new int [icsHelper.getNumWindowGroups()][];
		int numSec[] = new int [icsHelper.getNumWindowGroups()];
		for (int g = 0; g < icsHelper.getNumWindowGroups(); g++) {
			int k = 0;
			int i = 0;
			sectCb[g] = new int[icsHelper.getMaxSfb()];
			sectStart[g] = new int[icsHelper.getMaxSfb()];
			sectEnd[g] = new int[icsHelper.getMaxSfb()];
			while (k < icsHelper.getMaxSfb()) {
				sectCb[g][i] = bits.readBits(4);
				int sectLen = 0;
				int sectLenIncr;
				while ((sectLenIncr = bits.readBits(sectLenIncrBits)) == sectEscVal) {
					sectLen += sectEscVal;
				}
				sectLen += sectLenIncr;
				sectStart[g][i] = k;
				sectEnd[g][i] = k+sectLen;
				sfbCb[g] = new int[k+sectLen];
				for (int sfb = k; sfb < k+sectLen; sfb++)
					sfbCb[g][sfb] = sectCb[g][i];
				k += sectLen;
				i++;
			}
			numSec[g] = i;
		}
	}
/*
	private void parseScaleFactorData(BitBuf bits, IndividualChannelStreamHelper icsHelper) {
		for (int g = 0; g < icsHelper.getNumWindowGroups(); g++) {
			for (int sfb = 0; sfb < icsHelper.getMaxSfb(); sfb++) {
				if (sfb_cb[g][sfb] != ZERO_HCB) {
					if (is_intensity(g,sfb))
						hcod_sf[dpcm_is_position[g][sfb]];
					else
						hcod_sf[dpcm_sf[g][sfb]];
				}
			}
		}
	}
*/
	private IndividualChannelStreamHelper parseIcsInfo(BitBuf bits) {
		int reservedBit = bits.readBits(1);
		int winSeqVal = bits.readBits(2);
		ChannelPairElementAacBlock.WindowSequence winSequence
			= ChannelPairElementAacBlock.WindowSequence.valueOf(winSeqVal);
		// TODO: validate window sequence transitions
		System.out.print(" reserved="+reservedBit
				+" windowSequence="+winSequence);
		int maxSfb;
		Integer scaleFactorGrouping = null;
		if (winSequence == WindowSequence.EIGHT_SHORT_SEQUENCE) {
			maxSfb = bits.readBits(4);
			scaleFactorGrouping = bits.readBits(7);
		} else {
			maxSfb = bits.readBits(6);
			int predictorDataPresent = bits.readBits(1);
			if (predictorDataPresent == 1) {
				int predictorReset = bits.readBits(1);
				if (predictorReset == 1) {
					int predictorResetGroupNumber = bits.readBits(5);
				}
				final int PRED_SFB_MAX = PredictorInfo.getPredSfbMax(samplingFrequencyIndex);
				boolean[] predictionUsed = new boolean[Math.min(maxSfb, PRED_SFB_MAX)];
				for (int sfb = 0; sfb < Math.min(maxSfb, PRED_SFB_MAX); sfb++) {
					predictionUsed[sfb] = bits.readBits(1) == 1;
				}
			}
		}
		IndividualChannelStreamHelper h = new IndividualChannelStreamHelper(winSequence, scaleFactorGrouping, maxSfb, samplingFrequencyIndex);
		return h;
	}
}
