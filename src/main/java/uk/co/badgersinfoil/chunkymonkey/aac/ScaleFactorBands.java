package uk.co.badgersinfoil.chunkymonkey.aac;

/**
 * Static data from the AAC spec.
 */
public final class ScaleFactorBands {
	public static int getNumSwbShortWindow(SamplingFrequencyIndex freq) {
		switch (freq) {
		case FREQ_32000_HZ:
		case FREQ_44100_HZ:
		case FREQ_48000_HZ:
			return 14;
		case FREQ_8000_HZ:
			return 15;
		case FREQ_11025_HZ:
		case FREQ_12000_HZ:
		case FREQ_16000_HZ:
			return 15;
		case FREQ_22050_HZ:
		case FREQ_24000_HZ:
			return 15;
		case FREQ_64000_HZ:
			return 12;
		case FREQ_88200_HZ:
		case FREQ_96000_HZ:
			return 12;
		default:
			throw new Error("Unhandled freq: "+freq);
		}
	}

	public static int getNumSwbLongWindow(SamplingFrequencyIndex freq) {
		switch (freq) {
		case FREQ_44100_HZ:
		case FREQ_48000_HZ:
			return 49;
		case FREQ_32000_HZ:
			return 51;
		case FREQ_8000_HZ:
			return 40;
		case FREQ_11025_HZ:
		case FREQ_12000_HZ:
		case FREQ_16000_HZ:
			return 43;
		case FREQ_22050_HZ:
		case FREQ_24000_HZ:
			return 48;
		case FREQ_64000_HZ:
			return 47;
		case FREQ_88200_HZ:
		case FREQ_96000_HZ:
			return 41;
		default:
			throw new Error("Unhandled freq: "+freq);
		}
	}

	public static int getSwbOffsetLongWindow(SamplingFrequencyIndex freq, int swb) {
		if (swb < 0) {
			throw new IllegalArgumentException("swb must be 0 or greater: "+swb);
		}
		switch (freq) {
		case FREQ_44100_HZ:
		case FREQ_48000_HZ:
			if (swb > 48) {
				return 1024;
			}
			return new int[] {
				0,
				4,
				8,
				12,
				16,
				20,
				24,
				28,
				32,
				36,
				40,
				48,
				56,
				64,
				72,
				80,
				88,
				96,
				108,
				120,
				132,
				144,
				160,
				176,
				196,
				216,
				240,
				264,
				292,
				320,
				352,
				384,
				416,
				448,
				480,
				512,
				544,
				576,
				608,
				640,
				672,
				704,
				736,
				768,
				800,
				832,
				864,
				896,
				928
			}[swb];
		case FREQ_32000_HZ:
			if (swb > 50) {
				return 1024;
			}
			return new int[] {
				0,
				4,
				8,
				12,
				16,
				20,
				24,
				28,
				32,
				36,
				40,
				48,
				56,
				64,
				72,
				80,
				88,
				96,
				108,
				120,
				132,
				144,
				160,
				176,
				196,
				216,
				240,
				264,
				292,
				320,
				352,
				384,
				416,
				448,
				480,
				512,
				544,
				576,
				608,
				640,
				672,
				704,
				736,
				768,
				800,
				832,
				864,
				896,
				928,
				960,
				992
			}[swb];
		case FREQ_8000_HZ:
			if (swb > 39) {
				return 1024;
			}
			return new int[] {
				0,
				12,
				24,
				36,
				48,
				60,
				72,
				84,
				96,
				108,
				120,
				132,
				144,
				156,
				172,
				188,
				204,
				220,
				236,
				252,
				268,
				288,
				308,
				328,
				348,
				372,
				396,
				420,
				448,
				476,
				508,
				544,
				580,
				620,
				664,
				712,
				764,
				820,
				880,
				944
			}[swb];
		case FREQ_11025_HZ:
		case FREQ_12000_HZ:
		case FREQ_16000_HZ:
			if (swb > 42) {
				return 1024;
			}
			return new int[] {
				0,
				8,
				16,
				24,
				32,
				40,
				48,
				56,
				64,
				72,
				80,
				88,
				100,
				112,
				124,
				136,
				148,
				160,
				172,
				184,
				196,
				212,
				228,
				244,
				260,
				280,
				300,
				320,
				344,
				368,
				396,
				424,
				456,
				492,
				532,
				572,
				616,
				664,
				716,
				772,
				832,
				896,
				960
			}[swb];
		case FREQ_22050_HZ:
		case FREQ_24000_HZ:
			if (swb > 46) {
				return 1024;
			}
			return new int[] {
				0,
				4,
				8,
				12,
				16,
				20,
				24,
				28,
				32,
				36,
				40,
				44,
				52,
				60,
				68,
				76,
				84,
				92,
				100,
				108,
				116,
				124,
				136,
				148,
				160,
				172,
				188,
				204,
				220,
				240,
				260,
				284,
				308,
				336,
				364,
				396,
				432,
				468,
				508,
				552,
				600,
				652,
				704,
				768,
				832,
				896,
				960
			}[swb];
		case FREQ_64000_HZ:
			if (swb > 46) {
				return 1024;
			}
			return new int[] {
				0,
				4,
				8,
				12,
				16,
				20,
				24,
				28,
				32,
				36,
				40,
				44,
				48,
				52,
				56,
				64,
				72,
				80,
				88,
				100,
				112,
				124,
				140,
				156,
				172,
				192,
				216,
				240,
				268,
				304,
				344,
				384,
				424,
				464,
				504,
				544,
				584,
				624,
				664,
				704,
				744,
				784,
				824,
				864,
				904,
				944,
				984
			}[swb];
		case FREQ_88200_HZ:
		case FREQ_96000_HZ:
			if (swb > 40) {
				return 1024;
			}
			return new int[] {
				0,
				4,
				8,
				12,
				16,
				20,
				24,
				28,
				32,
				36,
				40,
				44,
				48,
				52,
				56,
				64,
				72,
				80,
				88,
				96,
				108,
				120,
				132,
				144,
				156,
				172,
				188,
				212,
				240,
				276,
				320,
				384,
				448,
				512,
				576,
				640,
				704,
				768,
				832,
				896,
				960
			}[swb];
		default:
			throw new Error("Unhandled freq: "+freq);	
		}
	}

	public static int getSwbOffsetShortWindow(SamplingFrequencyIndex freq, int swb) {
		if (swb < 0) {
			throw new IllegalArgumentException("swb must be 0 or greater: "+swb);
		}
		switch (freq) {
		case FREQ_32000_HZ:
		case FREQ_44100_HZ:
		case FREQ_48000_HZ:
			if (swb > 13) {
				return 128;
			}
			return new int[] {
				0,
				4,
				8,
				12,
				16,
				20,
				28,
				36,
				44,
				56,
				68,
				80,
				96,
				112
			}[swb];
		case FREQ_8000_HZ:
			if (swb > 14) {
				return 128;
			}
			return new int[] {
				0,
				4,
				8,
				12,
				16,
				20,
				24,
				28,
				36,
				44,
				52,
				60,
				72,
				88,
				108
			}[swb];
		case FREQ_11025_HZ:
		case FREQ_12000_HZ:
		case FREQ_16000_HZ:
			if (swb > 14) {
				return 128;
			}
			return new int[] {
				0,
				4,
				8,
				12,
				16,
				20,
				24,
				28,
				32,
				40,
				48,
				60,
				72,
				88,
				108
			}[swb];
		case FREQ_22050_HZ:
		case FREQ_24000_HZ:
			if (swb > 14) {
				return 128;
			}
			return new int[] {
				0,
				4,
				8,
				12,
				16,
				20,
				24,
				28,
				36,
				44,
				52,
				64,
				76,
				92,
				108
			}[swb];
		case FREQ_88200_HZ:
		case FREQ_96000_HZ:
			if (swb > 11) {
				return 128;
			}
			return new int[] {
				0,
				4,
				8,
				12,
				16,
				20,
				24,
				32,
				40,
				48,
				64,
				92
			}[swb];
		default:
			throw new Error("Unhandled freq: "+freq);	
		}
	}
}