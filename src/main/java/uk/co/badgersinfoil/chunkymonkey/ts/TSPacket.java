package uk.co.badgersinfoil.chunkymonkey.ts;

import io.netty.buffer.ByteBuf;
import uk.co.badgersinfoil.chunkymonkey.Locator;

public class TSPacket {
	public static final int TS_PACKET_LENGTH = 188;


	public static class TSPacketLocator implements Locator {

		private long packetNo;
		private int pid;
		private Locator parent;

		public TSPacketLocator(Locator parent, long packetNo, int pid) {
			this.parent = parent;
			this.packetNo = packetNo;
			this.pid = pid;
		}
		
		public long getPacketNo() {
			return packetNo;
		}
		
		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			b.append("TS Packet#").append(packetNo)
			 .append("[PID=").append(pid).append("]");
			Locator parent = getParent();
			if (parent != null) {
				b.append("\n  at ");
				b.append(parent);
			}
			return b.toString();
		}

		@Override
		public Locator getParent() {
			return parent;
		}
	}

	public static enum AdaptationFieldControl {
		RESERVED(0, false, false),
		PAYLOAD_ONLY(1, false, true),
		ADAPTATIONFIELD_ONLY(2, true, false),
		ADAPTATIONFIELD_AND_PAYLOAD(3, true, true);
		
		private int val;
		private boolean adaption;
		private boolean content;
		private AdaptationFieldControl(int val, boolean adaption, boolean content) {
			this.val = val;
			this.adaption = adaption;
			this.content = content;
		}
		public int getVal() {
			return val;
		}
		public boolean adaptionFieldPresent() {
			return adaption;
		}
		public boolean contentPresent() {
			return content;
		}
		public static AdaptationFieldControl valueOf(int i) {
			switch (i) {
			case 0:
				return RESERVED;
			case 1:
				return PAYLOAD_ONLY;
			case 2:
				return ADAPTATIONFIELD_ONLY;
			case 3:
				return ADAPTATIONFIELD_AND_PAYLOAD;
			}
			throw new IllegalArgumentException("Bad control value "+i);
		}
	}

	public class AdaptationField {
		public int length() {
			return buf.getByte(4) & 0b11111111;
		}
		
		public boolean discontinuityIndicator() {
			return length() > 0
			       && 0 != (buf.getByte(5) & 0b10000000);
		}
		
		public boolean randomAccessIndicator() {
			return length() > 0
			       && 0 != (buf.getByte(5) & 0b01000000);
		}
		
		public int elementryStreamPriority() {
			return length() > 0 ? (buf.getByte(5) & 0b00100000) >> 5 : 0;
		}

		public boolean pcrFlag() {
			return length() > 0
			       && 0 != (buf.getByte(5) & 0b00010000);
		}

		public boolean opcrFlag() {
			return length() > 0
			       && 0 != (buf.getByte(5) & 0b00001000);
		}

		public boolean splicingPointFlag() {
			return length() > 0
			       && 0 != (buf.getByte(5) & 0b00000100);
		}
		public boolean transportPrivateDataFlag() {
			return length() > 0
			       && 0 != (buf.getByte(5) & 0b00000010);
		}
		public boolean adaptationFieldExtensionFlag() {
			return length() > 0
			       && 0 != (buf.getByte(5) & 0b00000010);
		}
		public ProgramClockReference pcr() {
			if (!pcrFlag()) {
				throw new RuntimeException("No PCR field in this packet's adaptation field");
			}
			long pcrBase = (buf.getByte(6) & 0b11111111L) << 25
			              |(buf.getByte(7) & 0b11111111L) << 17
			              |(buf.getByte(8) & 0b11111111L) << 9
			              |(buf.getByte(9) & 0b11111111L) << 1
			              |(buf.getByte(10)& 0b10000000L) >> 7;
			int pcrExtension = (buf.getByte(10) & 0b00000001) << 8
					  |(buf.getByte(11) & 0b11111111);
			return new ProgramClockReference(pcrBase, pcrExtension);
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder()
				.append("[length=").append(length())
				.append(" randomAccessIndicator=").append(randomAccessIndicator())
				.append(" discontinuityIndicator=").append(discontinuityIndicator())
				.append(" elementryStreamPriority=").append(elementryStreamPriority())
				.append(" pcrFlag=").append(pcrFlag())
				.append(" opcrFlag=").append(opcrFlag())
				.append(" splicingPointFlag=").append(splicingPointFlag())
				.append(" transportPrivateDataFlag=").append(transportPrivateDataFlag())
				.append(" adaptationFieldExtensionFlag=").append(adaptationFieldExtensionFlag());
			if (pcrFlag()) {
				b.append(" pcr="+pcr());
			}
			b.append("]");
			return b.toString();
		}
	}

	public static class ProgramClockReference {

		private long base;
		private int extension;

		public ProgramClockReference(long base, int extension) {
			this.base = base;
			this.extension = extension;
		}
		public long getPcrBase() {
			return base;
		}
		public int getPcrExtension() {
			return extension;
		}
		@Override
		public String toString() {
			return "PCR:"+toSexidecimalString()+" (base="+base+" extension="+extension+")";
		}
		public long toNanoseconds() {
			return (base * 300L + extension) / 27L;
		}
		public String toSexidecimalString() {
			long nanos = toNanoseconds();
			return String.format("%02d:%02d:%02d.%06d", nanos/(60*60*1000000L),
			                                            (nanos/(60*1000000))%60,
			                                            (nanos/1000000)%60,
			                                            (nanos)%1000000);
		}
	}

	private AdaptationField adaptionField = new AdaptationField();
	private ByteBuf buf;
	private Locator parentLocator;
	private long packetNo;

	public TSPacket(Locator parentLocator, long packetNo, ByteBuf pk) {
		this.parentLocator = parentLocator;
		this.packetNo = packetNo;
		if (pk.readableBytes() != 188) {
			throw new IllegalArgumentException("ByteBuffer must contain 188 bytes, got: "+pk.readableBytes());
		}
		this.buf = pk;
	}
	public boolean synced() {
		return buf.getByte(0) == 0x47;  // an ASCII 'G'
	}
	public boolean transportErrorIndicator() {
		return 0 != (buf.getByte(1) & 0b10000000);
	}
	public boolean payloadUnitStartIndicator() {
		return 0 != (buf.getByte(1) & 0b01000000);
	}
	public boolean transportPriority() {
		return 0 != (buf.getByte(1) & 0b00100000);
	}
	public int PID() {
		return (buf.getByte(1) & 0b00011111) << 8
		      |(buf.getByte(2) & 0b11111111);
	}
	// TODO: scrambling control bits
	public AdaptationFieldControl adaptionControl() {
		return AdaptationFieldControl.valueOf(buf.getByte(3) >> 4 & 0b11);
	}
	public int continuityCounter() {
		return buf.getByte(3) & 0b00001111;
	}

	public AdaptationField getAdaptationField() {
		if (adaptionControl().adaptionFieldPresent()) {
			return adaptionField;
		}
		throw new RuntimeException("No adaption field present");
	}
	
	public ByteBuf getPayload() {
		return buf.slice(contentStart(), getPayloadLength());
	}
	public int getPayloadLength() {
		return TS_PACKET_LENGTH - contentStart();
	}

	@Override
	public String toString() {
		return "synced="+synced()
		      +" transportErrorIndicator="+transportErrorIndicator()
		      +" payloadUnitStartIndicator="+payloadUnitStartIndicator()
		      +" transportPriority="+transportPriority()
		      +" PID="+PID()
		      +" adaptionControl="+adaptionControl()
		      +" continuityCounter="+continuityCounter()
		      + (adaptionControl().adaptionFieldPresent() ? " adaptionField="+getAdaptationField(): " <no adaption field>");
	}
	public int contentStart() {
		if (!adaptionControl().contentPresent()) {
			throw new RuntimeException("No content in this packet");
		}
		final int TSPACKET_FIXED_HEADER_LENGTH = 4;
		if (adaptionControl().adaptionFieldPresent()) {
			final int ADAPTIONFIELD_LENGTH_HEADER_LENGTH = 1;
			return TSPACKET_FIXED_HEADER_LENGTH
			     + ADAPTIONFIELD_LENGTH_HEADER_LENGTH
			     + getAdaptationField().length();
		}
		return TSPACKET_FIXED_HEADER_LENGTH;
	}

	public TSPacketLocator getLocator() {
		return new TSPacketLocator(parentLocator, packetNo, PID());
	}

	/**
	 * Returns the raw 188 byte buffer backing this packet, including
	 * headers and payload.
	 */
	public ByteBuf getBuffer() {
		return buf;
	}
}