package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import uk.co.badgersinfoil.chunkymonkey.Locator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class PESPacket {

	public static class Timestamp {

		private long ts;
		private int expectedCheck;
		private int actualCheck;
		private int checkBit1;
		private int checkBit2;
		private int checkBit3;

		public Timestamp(long ts, int expectedCheck, int actualCheck,
				int checkBit1, int checkBit2, int checkBit3) {
					this.ts = ts;
					this.expectedCheck = expectedCheck;
					this.actualCheck = actualCheck;
					this.checkBit1 = checkBit1;
					this.checkBit2 = checkBit2;
					this.checkBit3 = checkBit3;
		}
		
		public boolean isValid() {
			return expectedCheck == actualCheck
			       && checkBit1 == 1
			       && checkBit2 == 1
			       && checkBit3 == 1;
		}
		
		public long getTs() {
			if (!isValid()) {
				throw new RuntimeException("Can't retreve timestamp value when isValid() is false");
			}
			return ts;
		}

		public static Timestamp parse(final ByteBuf buf, final int offset, final int expectedCheck) {
			int actualCheck = buf.getUnsignedByte(offset) >> 4;
//			long ts = ((long)buf.getUnsignedByte(offset) & 0x1110) << 29;
			int checkBit1 = buf.getUnsignedByte(offset) & 1;
//			ts |= (buf.getUnsignedShort(offset+1) & 0b1111111111111110) << 14;
			int checkBit2 = buf.getUnsignedByte(offset+2) & 1;
//			ts |= (buf.getUnsignedShort(offset+3) & 0b1111111111111110) >> 1;
			int checkBit3 = buf.getUnsignedByte(offset+4) & 1;
			long ts = (((long) buf.getByte(offset) & 0x0e) << 29) | ((buf.getByte(offset+1) & 0xff) << 22) | (((buf.getByte(offset+2) & 0xff) >> 1) << 15)
			                | ((buf.getByte(offset+3) & 0xff) << 7) | ((buf.getByte(offset+4) & 0xff) >> 1);
			return new Timestamp(ts, expectedCheck, actualCheck, checkBit1, checkBit2, checkBit3);
		}

		@Override
		public String toString() {
			if (isValid()) {
				return String.valueOf(ts);
			}
			StringBuilder b = new StringBuilder("<invalid ts field ");
			b.append(">");
			return b.toString();
		}
		
		public String toSexidecimal() {
			long t = getTs();
			final long TICKS = 90000;
			final long MILLIS = 1000;
			long hours  = t / TICKS / 60 / 60;
			long mins   = t / TICKS / 60 % 60;
			long secs   = t / TICKS % 60;
			long millis = t * MILLIS / TICKS % MILLIS;
			return String.format("%02d:%02d:%02d.%03d", hours, mins, secs, millis);
		}
	}

	private static final int PROGRAM_STREAM_MAP       = 0b1011_1100;
	private static final int PRIVATE_STREAM_2         = 0b1011_1111;
	private static final int ECM_STREAM               = 0b1111_0000;
	private static final int EMM_STREAM               = 0b1111_0001;
	private static final int PROGRAM_STREAM_DIRECTORY = 0b1111_1111;
	private static final int DSMCC_STREAM             = 0b1111_0010;
	private static final int H222_1_E_STREAM          = 0b1111_1000;
	private static final Set<Integer> UNPARSED_STREAM_IDS;
	static {
		Set<Integer> ids = new HashSet<Integer>();
		ids.add(PROGRAM_STREAM_MAP);
		ids.add(PRIVATE_STREAM_2);
		ids.add(ECM_STREAM);
		ids.add(EMM_STREAM);
		ids.add(PROGRAM_STREAM_DIRECTORY);
		ids.add(DSMCC_STREAM);
		ids.add(H222_1_E_STREAM);
		UNPARSED_STREAM_IDS = Collections.unmodifiableSet(ids);
	}

	private ByteBuf buf;
	private Locator loc;

	public PESPacket(Locator loc, ByteBuf buf) {
		this.loc = loc;
		//FIXME: massive hack! how does the buffer change under us?
		this.buf = buf.copy();
	}
	
	public int packetStartCodePrefix() {
		return (buf.getByte(0) & 0xff) << 16
		      |(buf.getByte(1) & 0xff) << 8
		      |(buf.getByte(2) & 0xff);
	}
	public int streamId() {
		return buf.getByte(3) & 0xff;
	}
	public int packetLength() {
		return (buf.getByte(4) & 0xff) << 8
		      |(buf.getByte(5) & 0xff);
	}

	public enum PtsDtsFlags {
		NONE(false, false),
		PTS_ONLY(true, false),
		PTS_AND_DTS(true, true),
		FORBIDDEN(false, false);

		private boolean pts;
		private boolean dts;

		private PtsDtsFlags(boolean pts, boolean dts) {
			this.pts = pts;
			this.dts = dts;
		}
		
		public boolean isPtsPresent() {
			return pts;
		}
		public boolean isDtsPresent() {
			return dts;
		}

		/**
		 * @param flags 2-bit flags value from parsed PES header
		 */
		public static PtsDtsFlags forFlags(int flags) {
			switch (flags) {
			case 0b00: return NONE;
			case 0b10: return PTS_ONLY;
			case 0b11: return PTS_AND_DTS;
			case 0b01: return FORBIDDEN;
			default: throw new IllegalArgumentException("Invalid flags value "+flags);
			}
		}
	}

	public class Parsed {
		/**
		 * Should always return 2.
		 */
		public int checkBits() {
			return (buf.getByte(6) & 0b11000000) >> 6;
		}
		public int pesScrambingControl() {
			return (buf.getByte(6) & 0b00110000) >> 4;
		}
		public int pesPriority() {
			return (buf.getByte(6) & 0b00001000) >> 3;
		}
		public int dataAlignmentIndicator() {
			return (buf.getByte(6) & 0b00000100) >> 2;
		}
		public int copyright() {
			return (buf.getByte(6) & 0b00000010) >> 1;
		}
		public int originalOrCopy() {
			return (buf.getByte(6) & 0b00000001);
		}
		public PtsDtsFlags ptsDdsFlags() {
			return PtsDtsFlags.forFlags((buf.getByte(7) & 0b11000000) >> 6);
		}
		public boolean esrcFlag() {
			return 0 != (buf.getByte(7) & 0b00100000);
		}
		public boolean esRateFlag() {
			return 0 != (buf.getByte(7) & 0b00010000);
		}
		public boolean dsmTrickModeFlag() {
			return 0 != (buf.getByte(7) & 0b00001000);
		}
		public boolean additionalCopyInfoFlag() {
			return 0 != (buf.getByte(7) & 0b00000100);
		}
		public boolean pesCRCFlag() {
			return 0 != (buf.getByte(7) & 0b00000010);
		}
		public boolean pesExtensionFlag() {
			return 0 != (buf.getByte(7) & 0b00000001);
		}
		public int pesHeaderDataLength() {
			return buf.getUnsignedByte(8);
		}
		public Timestamp pts() {
			switch (ptsDdsFlags()) {
			case FORBIDDEN:
			case NONE:
				throw new RuntimeException("can only be called when ptsDdsFlags() allows");
			case PTS_ONLY:
				return Timestamp.parse(buf, 9, 0b0010);
			case PTS_AND_DTS:
				return Timestamp.parse(buf, 9, 0b0011);
			default:
				throw new Error("Imposible case");
			}
		}
		public Timestamp dts() {
			switch (ptsDdsFlags()) {
			case FORBIDDEN:
			case NONE:
			case PTS_ONLY:
				throw new RuntimeException("can only be called when ptsDdsFlags() allows");
			case PTS_AND_DTS:
				return Timestamp.parse(buf, 14, 0b0001);
			default:
				throw new Error("Imposible case");
			}
		}
		// TODO: expose rest of header data
		public ByteBuf getContent() {
			final int FIXED_PES_HEADER_LENGTH = 9;
			final int offset = pesHeaderDataLength()+FIXED_PES_HEADER_LENGTH;
			return buf.slice(offset,
			                 buf.readableBytes() - offset);
		}
		
		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			b.append("checkBits=0b").append(Integer.toBinaryString(checkBits()))
			 .append(" pesScrambingControl=").append(pesScrambingControl())
			 .append(" pesPriority=").append(pesPriority())
			 .append(" dataAlignmentIndicator=").append(dataAlignmentIndicator())
			 .append(" copyright=").append(copyright())
			 .append(" originalOrCopy=").append(originalOrCopy())
			 .append(" ptsDdsFlags=").append(ptsDdsFlags())
			 .append(" esrcFlag=").append(esrcFlag())
			 .append(" esRateFlag=").append(esRateFlag())
			 .append(" dsmTrickModeFlag=").append(dsmTrickModeFlag())
			 .append(" additionalCopyInfoFlag=").append(additionalCopyInfoFlag())
			 .append(" pesCRCFlag=").append(pesCRCFlag())
			 .append(" pesExtensionFlag=").append(pesExtensionFlag())
			 .append(" pesHeaderDataLength=").append(pesHeaderDataLength())
			 .append(" data=").append(ByteBufUtil.hexDump(getContent()));
			if (ptsDdsFlags().isPtsPresent()) {
				b.append(" pts=").append(pts());
			}
			if (ptsDdsFlags().isDtsPresent()) {
				b.append(" dts=").append(dts());
			}
			return b.toString();
		}
	}


	public Parsed getParsedPESPaload() {
		if (!isParsed()) {
			throw new RuntimeException("stream_id "+streamId()+" is not parsed, use getUnparsedData() instead");
		}
		return new Parsed();
	}
	public boolean isParsed() {
		return !UNPARSED_STREAM_IDS.contains(streamId());
	}

	public ByteBuf getUnparsedData() {
		final int PES_COMMON_HEADER_LENGTH = 5;
		return buf.slice(PES_COMMON_HEADER_LENGTH, buf.readableBytes() - PES_COMMON_HEADER_LENGTH);
	}
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("packetStartCodePrefix=").append(packetStartCodePrefix())
		 .append(" streamId=").append(streamId())
		 .append(" packetLength=").append(packetLength());
		if (isParsed()) {
			b.append(" ").append(getParsedPESPaload().toString());
		}
		return b.toString();
	}

	public Locator getLocator() {
		return loc;
	}
}
