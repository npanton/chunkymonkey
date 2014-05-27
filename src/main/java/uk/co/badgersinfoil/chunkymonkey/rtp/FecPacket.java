package uk.co.badgersinfoil.chunkymonkey.rtp;

import io.netty.buffer.ByteBuf;

/**
 * Represents the payload of a Forward Error Correction RTP packet.  (Does
 * not include the RTP headers.)
 *
 * <pre>
 *   RtpPacket rtp = ...get RTP packet of the right type from somewhere...;
 *   FecPacket fec = new FecPacket(rtp.payload());
 * </pre>
 */
public class FecPacket {

	/**
	 * Orientation of a series of media packets to which an FEC packet
	 * applies.
	 */
	public static enum Direction {
		COLS, ROWS
	}

	/**
	 * The types of correction that an FEC packet might indicate (we only
	 * support XOR).
	 */
	public static enum CorrectionType {
		XOR,
		HAMMING,
		REED_SOLOMON,
		UNDEFINED3,
		UNDEFINED4,
		UNDEFINED5,
		UNDEFINED6,
		UNDEFINED7
	}

	private ByteBuf buf;

	public FecPacket(ByteBuf buf) {
		this.buf = buf;
	}
	/**
	 * Returns the minimum sequence number of the series of media packets
	 * with which this FEC packet is associated.
	 */
	public int snBaseLowBits() {
		return buf.getUnsignedShort(0);
	}
	public int lengthRecovery() {
		return buf.getUnsignedShort(2);
	}
	/**
	 * True if the RFC 2733 'X' flag is set (we do not support FEC where
	 * this flag is false).
	 */
	public boolean rfc2733Extended() {
		return 0 != (buf.getUnsignedByte(4) & 0b10000000);
	}
	public int ptRecovery() {
		return buf.getUnsignedByte(4) & 0b01111111;
	}
	public int mask() {
		return buf.getUnsignedMedium(5);
	}
	public long tsRecovery() {
		return buf.getUnsignedInt(8);
	}
	private void assertRfc2733Extended() {
		if (!rfc2733Extended()) {
			throw new IllegalStateException("RFC 2733 extension flag not set");
		}
	}
	/**
	 * @throws IllegalStateException if {@link #rfc2733Extended()} is
	 * false for this packet.
	 */
	public boolean mpegFecExtended() {
		assertRfc2733Extended();
		return 0 != (buf.getUnsignedByte(12) & 0b10000000);
	}
	/**
	 * @throws IllegalStateException if {@link #rfc2733Extended()} is
	 * false for this packet.
	 */
	public FecPacket.Direction direction() {
		assertRfc2733Extended();
		return 0 == (buf.getUnsignedByte(12) & 0b10000000)
			? Direction.COLS
			: Direction.ROWS;
	}
	/**
	 * @throws IllegalStateException if {@link #rfc2733Extended()} is
	 * false for this packet.
	 */
	public FecPacket.CorrectionType type() {
		assertRfc2733Extended();
		return CorrectionType.values()[buf.getUnsignedByte(12) >> 3 & 0b111];
	}
	/**
	 * @throws IllegalStateException if {@link #rfc2733Extended()} is
	 * false for this packet.
	 */
	public int index() {
		assertRfc2733Extended();
		return buf.getUnsignedByte(12) & 0b111;
	}
	/**
	 * @throws IllegalStateException if {@link #rfc2733Extended()} is
	 * false for this packet.
	 */
	public int offset() {
		assertRfc2733Extended();
		return buf.getUnsignedByte(13);
	}
	/**
	 * @throws IllegalStateException if {@link #rfc2733Extended()} is
	 * false for this packet.
	 */
	public int numberAssociations() {
		assertRfc2733Extended();
		return buf.getUnsignedByte(14);
	}
	/**
	 * Returns the high bits of the minimum sequence number of the media
	 * packet with which this FEC packet is associated (our implementation
	 * of FEC implicitly assumes this value is always 0).
	 *
	 * @throws IllegalStateException if {@link #rfc2733Extended()} is
	 * false for this packet.
	 */
	public int snBaseExtBits() {
		assertRfc2733Extended();
		return buf.getUnsignedByte(15);
	}
	public ByteBuf payload() {
		if (rfc2733Extended()) {
			return buf.slice(16, buf.readableBytes() - 16);
		} else {
			return buf.slice(12, buf.readableBytes() - 12);
		}
	}
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("snBaseLowBits=").append(snBaseLowBits())
		 .append(" lengthRecovery=").append(lengthRecovery())
		 .append(" rfc2733Extended=").append(rfc2733Extended())
		 .append(" ptRecovery=").append(ptRecovery())
		 .append(" mask=").append(mask())
		 .append(" tsRecovery=").append(tsRecovery());
		if (rfc2733Extended()) {
			b.append(" mpegFecExtended=").append(mpegFecExtended())
			 .append(" direction=").append(direction())
			 .append(" type=").append(type())
			 .append(" index=").append(index())
			 .append(" offset=").append(offset())
			 .append(" numberAssociations=").append(numberAssociations())
			 .append(" snBaseExtBits=").append(snBaseExtBits());
		}
		return b.toString();
	}
}