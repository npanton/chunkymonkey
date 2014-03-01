package uk.co.badgersinfoil.chunkymonkey.aac;

public enum SamplingFrequencyIndex {
	FREQ_96000_HZ(0x0, 96000),
	FREQ_88200_HZ(0x1, 88200),
	FREQ_64000_HZ(0x2, 64000),
	FREQ_48000_HZ(0x3, 48000),
	FREQ_44100_HZ(0x4, 44100),
	FREQ_32000_HZ(0x5, 32000),
	FREQ_24000_HZ(0x6, 24000),
	FREQ_22050_HZ(0x7, 22050),
	FREQ_16000_HZ(0x8, 16000),
	FREQ_12000_HZ(0x9, 12000),
	FREQ_11025_HZ(0xa, 11025),
	FREQ_8000_HZ(0xb, 8000),
	FREQ_reserved_0xC(0xc),
	FREQ_reserved_0xD(0xd),
	FREQ_reserved_0xE(0xe),
	FREQ_reserved_0xF(0xf);

	private int index;
	private int frequency;

	private SamplingFrequencyIndex(int index, int frequency) {
		this.index = index;
		this.frequency = frequency;
	}

	private SamplingFrequencyIndex(int index) {
		this.index = index;
		this.frequency = -1;
	}

	public int getIndex() {
		return index;
	}
	public int getFrequency() {
		if (frequency == -1) {
			throw new RuntimeException("This is a 'reserved' index, no frequency available");
		}
		return frequency;
	}
	public boolean isReserved() {
		return frequency == -1;
	}
	
	public static SamplingFrequencyIndex forIndex(int index) {
		switch (index) {
		case 0x0: return FREQ_96000_HZ;
		case 0x1: return FREQ_88200_HZ;
		case 0x2: return FREQ_64000_HZ;
		case 0x3: return FREQ_48000_HZ;
		case 0x4: return FREQ_44100_HZ;
		case 0x5: return FREQ_32000_HZ;
		case 0x6: return FREQ_24000_HZ;
		case 0x7: return FREQ_22050_HZ;
		case 0x8: return FREQ_16000_HZ;
		case 0x9: return FREQ_12000_HZ;
		case 0xa: return FREQ_11025_HZ;
		case 0xb: return FREQ_8000_HZ;
		case 0xc: return FREQ_reserved_0xC;
		case 0xd: return FREQ_reserved_0xD;
		case 0xe: return FREQ_reserved_0xE;
		case 0xf: return FREQ_reserved_0xF;
		default: throw new IllegalArgumentException("Invalid index (outside 0x0-0xf): "+index);
		}
	}
	
	public static SamplingFrequencyIndex forFrequency(int f) {
		switch (f) {
		case 96000: return FREQ_96000_HZ;
		case 88200: return FREQ_88200_HZ;
		case 64000: return FREQ_64000_HZ;
		case 48000: return FREQ_48000_HZ;
		case 44100: return FREQ_44100_HZ;
		case 32000: return FREQ_32000_HZ;
		case 24000: return FREQ_24000_HZ;
		case 22050: return FREQ_22050_HZ;
		case 16000: return FREQ_16000_HZ;
		case 12000: return FREQ_12000_HZ;
		case 11025: return FREQ_11025_HZ;
		case 8000: return FREQ_8000_HZ;
		default: throw new IllegalArgumentException("Invalid frequency: "+f);
		}
	}
}
