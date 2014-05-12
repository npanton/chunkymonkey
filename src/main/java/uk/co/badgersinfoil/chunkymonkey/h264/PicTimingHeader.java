package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.MediaTimestamp;
import uk.co.badgersinfoil.chunkymonkey.MediaUnits;
import uk.co.badgersinfoil.chunkymonkey.h264.SeqParamSet.HrdParameters;
import uk.co.badgersinfoil.chunkymonkey.h264.SeqParamSet.TimingInfo;
import uk.co.badgersinfoil.chunkymonkey.h264.SeqParamSet.VuiParameters;
import io.netty.buffer.ByteBuf;

public class PicTimingHeader {

	public static class ClockTimestamp {
		public static enum CtType {
			PROGRESSIVE,
			INTERLACED,
			UNKNOWN,
			RESERVED;

			public static CtType forKey(int k) {
				switch (k) {
				case 0: return PROGRESSIVE;
				case 1: return INTERLACED;
				case 2: return UNKNOWN;
				case 3: return RESERVED;
				default: throw new IllegalArgumentException("Invalid key: "+k);
				}
			}
		}
		private VuiParameters vuiParameters;
		private CtType ctType;
		private boolean nuitFieldBasedFlag;
		private int countingType;
		private boolean fullTimestampFlag;
		private boolean discontinuityFlag;
		private boolean cntDroppedFlag;
		private int nFrames;
		private int secondsValue = -1;
		private int minutesValue = -1;
		private int hoursValue = -1;
		private int timeOffset = 0;

		public ClockTimestamp(H264BitBuf bits, VuiParameters vuiParameters, int timeOffsetLength) {
			this.vuiParameters = vuiParameters;
			ctType = CtType.forKey(bits.readBits(2));
			nuitFieldBasedFlag = bits.readBool();
			countingType = bits.readBits(5);
			fullTimestampFlag = bits.readBool();
			discontinuityFlag = bits.readBool();
			cntDroppedFlag = bits.readBool();
			nFrames = bits.readBits(8);
			if (fullTimestampFlag) {
				secondsValue = bits.readBits(6);
				minutesValue = bits.readBits(6);
				hoursValue = bits.readBits(5);
			} else {
				boolean secondsFlag = bits.readBool();
				if (secondsFlag) {
					secondsValue = bits.readBits(6);
					boolean minutesFlag = bits.readBool();
					if (minutesFlag) {
						minutesValue = bits.readBits(6);
						boolean hoursFlag = bits.readBool();
						if (hoursFlag) {
							hoursValue = bits.readBits(5);
						}
					}
				}
			}
			if (timeOffsetLength > 0) {
				timeOffset = bits.readBits(timeOffsetLength);
			}
		}

		public StringBuilder toString(StringBuilder b) {
			b.append(ctType());
			if (nuitFieldBasedFlag()) {
				b.append(" fieldBased");
			}
			b.append(" countType=").append(countingType());
			if (fullTimestampFlag) {
				b.append(" fullTS");
			}
			if (discontinuityFlag()) {
				b.append(" discontinuity");
			}
			if (cntDroppedFlag()) {
				b.append(" drop");
			}
			if (secondsValue() != -1) {
				b.append(" ");
				if (minutesValue() != -1) {
					if (hoursValue() != -1) {
						b.append(hoursValue()).append("h");
					}
					b.append(minutesValue()).append("m");
				}
				b.append(secondsValue()).append("s.");
			}
			b.append(nFrames()).append("f");
			b.append("+").append(timeOffset());
			return b;
		}
		@Override
		public String toString() {
			return toString(new StringBuilder()).toString();
		}

		public int timeOffset() {
			return timeOffset;
		}

		public int hoursValue() {
			return hoursValue;
		}

		public int minutesValue() {
			return minutesValue;
		}

		public int secondsValue() {
			return secondsValue;
		}

		public int nFrames() {
			return nFrames;
		}

		public boolean cntDroppedFlag() {
			return cntDroppedFlag;
		}

		public boolean discontinuityFlag() {
			return discontinuityFlag;
		}

		public int countingType() {
			return countingType;
		}

		public boolean nuitFieldBasedFlag() {
			return nuitFieldBasedFlag;
		}

		public CtType ctType() {
			return ctType;
		}

		public MediaTimestamp toClockTimestamp() {
			// spec:
			// clockTimestamp = ((hH * 60 + mM) * 60 + sS) * time_scale +
			//                    nFrames * (num_units_in_tick * (1 + nuit_field_based_flag)) + tOffset

			long result = 0;
			TimingInfo timingInfo = vuiParameters.timingInfo();
			long timeScale = timingInfo.timeScale();
			if (hoursValue != -1) {
				result += hoursValue * 60 * 60 * timeScale;
			}
			if (minutesValue != -1) {
				result += minutesValue * 60 * timeScale;
			}
			if (secondsValue != -1) {
				result += secondsValue * timeScale;
			}
			// TODO: in the example data we care about, interlacing
			// means we convert fields/sec to frames/sec by
			// dividing by two; we should derive this from the
			// metadata
			final long MAGIC_FACTOR = 2;
			result /= MAGIC_FACTOR ;
			result += nFrames * (timingInfo.numUnitsInTick()
			                  * (nuitFieldBasedFlag ? 2 : 1))
			          + timeOffset;

			// TODO: construct H264MediaUnits once per
			//       seq_param_set, not once per pic_timing,
			return new MediaTimestamp(result,
			                         new MediaUnits(1, timeScale / MAGIC_FACTOR, "h264_clock_ticks"));
		}
	}

	public static enum PicStruct {
		FRAME(0, 1),
		TOP_FIELD(1, 1),
		BOTTOM_FIELD(2, 1),
		TOP_FIELD_BOTTOM_FIELD(3, 2),
		BOTTOM_FIELD_TOP_FIELD(4, 2),
		TOP_FIELD_BOTTOM_FIELD_TOP_FIELD_REPEATED(5, 3),
		BOTTOM_FIELD_TOP_FIELD_BOTTOM_FIELD_REPEATED(6, 3),
		FRAME_DOUBLING(7, 2),
		FRAME_TRIPLING(8, 3),
		RESERVED9(9),
		RESERVED10(10),
		RESERVED11(11),
		RESERVED12(12),
		RESERVED13(13),
		RESERVED14(14),
		RESERVED15(15);

		private int key;
		private int numClockTS = -1;

		private PicStruct(int key) {
			this.key = key;
		}
		private PicStruct(int key, int numClockTS) {
			this.key = key;
			this.numClockTS = numClockTS;
		}
		public int numClockTS() {
			if (numClockTS == -1) {
				throw new IllegalStateException("No numClockTS defined for PicStruct:"+this.name());
			}
			return numClockTS;
		}

		public static PicStruct forVal(int v) {
			switch (v) {
			case 0: return FRAME;
			case 1: return TOP_FIELD;
			case 2: return BOTTOM_FIELD;
			case 3: return TOP_FIELD_BOTTOM_FIELD;
			case 4: return BOTTOM_FIELD_TOP_FIELD;
			case 5: return TOP_FIELD_BOTTOM_FIELD_TOP_FIELD_REPEATED;
			case 6: return BOTTOM_FIELD_TOP_FIELD_BOTTOM_FIELD_REPEATED;
			case 7: return FRAME_DOUBLING;
			case 8: return FRAME_TRIPLING;
			case 9: return RESERVED9;
			case 10: return RESERVED10;
			case 11: return RESERVED11;
			case 12: return RESERVED12;
			case 13: return RESERVED13;
			case 14: return RESERVED14;
			case 15: return	RESERVED15;
			default: throw new IllegalArgumentException("Invalid key "+v);
			}
		}
	}

	private int cpbRemovalDelay = -1;
	private int dpbOutputDelay = -1;
	private PicStruct picStruct;
	private ClockTimestamp[] clockTimestamps;

	public PicTimingHeader(ByteBuf buf, VuiParameters vuiParameters) {
		H264BitBuf bits = new H264BitBuf(buf);
		HrdParameters hrdParams = vuiParameters.nalHrdParameters() != null ? vuiParameters.nalHrdParameters() : vuiParameters.vclHrdParameters();
		int timeOffsetLength;
		if (hrdParams == null) {
			// TODO: spec suggests 24, other code suggests 0?
			timeOffsetLength = 24;
		} else {
			cpbRemovalDelay = bits.readBits(hrdParams.cpbRemovalDelayLengthMinus1() + 1);
			dpbOutputDelay = bits.readBits(hrdParams.dpbOutputDelayLengthMinus1() + 1);
			timeOffsetLength = hrdParams.timeOffsetLength();
		}
		if (vuiParameters.picStructPresentFlag()) {
			picStruct = PicStruct.forVal(bits.readBits(4));
			int numClockTS = picStruct.numClockTS();
			if (numClockTS != -1) {
				clockTimestamps = new ClockTimestamp[numClockTS];
				for (int i=0; i<numClockTS; i++) {
					boolean clockTimestampFlag = bits.readBool();
					if (clockTimestampFlag) {
						clockTimestamps[i] = new ClockTimestamp(bits, vuiParameters, timeOffsetLength);
					}
				}
			}
		}
//System.err.println("PicTimingHeader: remainging unread bits="+bits.readableBits());
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		if (cpbRemovalDelay() != -1) {
			b.append("cpbRemovalDelay=").append(cpbRemovalDelay());
		}
		if (dpbOutputDelay() != -1) {
			b.append(" dpbOutputDelay=").append(dpbOutputDelay());
		}
		b.append(picStruct());
		if (clockTimestamps != null) {
			b.append(" [");
			for (int i=0; i<clockTimestamps.length; i++) {
				if (i > 0) {
					b.append(", ");
				}
				if (clockTimestamps[i] == null) {
					b.append("N/A");
				} else {
					b.append("{ ");
					clockTimestamps[i].toString(b);
					b.append(" }");
				}
			}
			b.append(" ]");
		}
		return b.toString();
	}
	public PicStruct picStruct() {
		return picStruct;
	}

	public ClockTimestamp[] clockTimestamps() {
		return clockTimestamps;
	}

	public int cpbRemovalDelay() {
		return cpbRemovalDelay;
	}
	public int dpbOutputDelay() {
		return dpbOutputDelay;
	}
}
