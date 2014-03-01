package uk.co.badgersinfoil.chunkymonkey.ts;

public enum AudioObjectType {
	NULL(0),
	AAC_MAIN(1),
	AAC_LOW_COMPLEXITY(2),
	AAC_SCALABLE_SAMPLE_RATE(3),
	AAC_LONG_TERM_PREDICTION(4),
	SPECTRAL_BAND_REPLICATION(5),
	AAC_SCALABLE(6),
	TWIN_VQ(7),
	CELP(8),
	HXVC(9),
	RESERVED_10(10),
	RESERVED_11(11),
	TTSI(12),
	MAIN_SYNTHESIS(13),
	WAVETABLE_SYNTHESIS(14),
	GENERAL_MIDI(15),
	ALGORITHMIC_SYNTHESIS_AND_AUDIO_EFFECTS(16),
	ER_AAC_LOW_COMPLEXITY(17),
	RESERVED_18(18),
	ER_AAC_LONG_TERM_PREDICTION(19),
	ER_AAC_SCALABLE(20),
	ER_TWIN_VQ(21),
	ER_BSAC(22),
	ER_AAC_LOW_DELAY(23),
	ER_CELP(24),
	ER_HVXC(25),
	ER_HILN(26),
	ER_PARAMETRIC(27),
	SINUSOIDAL_CODING(28),
	PARAMETRIC_STERIO(29),
	MPEG_SURROUND(30),
	ESCAPE_VALUE(31),
	LAYER_1(32),
	LAYER_2(33),
	LAYER_3(34),
	DST(35),
	ALS(36),
	SLS(37),
	SLS_NON_CORE(38),
	ER_AAC_ELD(39),
	SMR(40),
	SMR_MAIN(41),
	USAC_NO_SBR(42),
	UAOC(43),
	LD_MPEG_SURROUND(44),
	USAC(45);
	
	private int index;

	private AudioObjectType(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
	public static AudioObjectType forIndex(int index) {
		switch (index) {
		case 0: return NULL;
		case 1: return AAC_MAIN;
		case 2: return AAC_LOW_COMPLEXITY;
		case 3: return AAC_SCALABLE_SAMPLE_RATE;
		case 4: return AAC_LONG_TERM_PREDICTION;
		case 5: return SPECTRAL_BAND_REPLICATION;
		case 6: return AAC_SCALABLE;
		case 7: return TWIN_VQ;
		case 8: return CELP;
		case 9: return HXVC;
		case 10: return RESERVED_10;
		case 11: return RESERVED_11;
		case 12: return TTSI;
		case 13: return MAIN_SYNTHESIS;
		case 14: return WAVETABLE_SYNTHESIS;
		case 15: return GENERAL_MIDI;
		case 16: return ALGORITHMIC_SYNTHESIS_AND_AUDIO_EFFECTS;
		case 17: return ER_AAC_LOW_COMPLEXITY;
		case 18: return RESERVED_18;
		case 19: return ER_AAC_LONG_TERM_PREDICTION;
		case 20: return ER_AAC_SCALABLE;
		case 21: return ER_TWIN_VQ;
		case 22: return ER_BSAC;
		case 23: return ER_AAC_LOW_DELAY;
		case 24: return ER_CELP;
		case 25: return ER_HVXC;
		case 26: return ER_HILN;
		case 27: return ER_PARAMETRIC;
		case 28: return SINUSOIDAL_CODING;
		case 29: return PARAMETRIC_STERIO;
		case 30: return MPEG_SURROUND;
		case 31: return ESCAPE_VALUE;
		case 32: return LAYER_1;
		case 33: return LAYER_2;
		case 34: return LAYER_3;
		case 35: return DST;
		case 36: return ALS;
		case 37: return SLS;
		case 38: return SLS_NON_CORE;
		case 39: return ER_AAC_ELD;
		case 40: return SMR;
		case 41: return SMR_MAIN;
		case 42: return USAC_NO_SBR;
		case 43: return UAOC;
		case 44: return LD_MPEG_SURROUND;
		case 45: return USAC;
		default: throw new IllegalArgumentException("Invalid index: " + index);
		}
	}
}
