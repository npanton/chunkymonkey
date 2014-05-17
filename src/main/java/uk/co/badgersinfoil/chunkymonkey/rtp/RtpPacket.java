package uk.co.badgersinfoil.chunkymonkey.rtp;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;

public class RtpPacket {

	private ByteBuf buf;

	public RtpPacket(ByteBuf buf) {
		this.buf = buf;
	}

	public int version() {
		return (buf.getByte(0) & 0b11000000) >> 6;
	}

	public boolean padding() {
		return (buf.getByte(0) & 0b00100000) != 0;
	}

	public boolean extension() {
		return (buf.getByte(0) & 0b00010000) != 0;
	}

	public int csrcCount() {
		return buf.getByte(0) & 0b00001111;
	}

	public boolean mark() {
		return (buf.getByte(1) & 0b10000000) != 0;
	}

	public int payloadType() {
		return buf.getByte(1) & 0b011111111;
	}

	public int sequenceNumber() {
		return buf.getUnsignedShort(2);
	}

	public long timestamp() {
		return buf.getUnsignedInt(4);
	}

	public long ssrc() {
		return buf.getUnsignedInt(8);
	}

	public int[] csrcs() {
		int c = csrcCount();
		int[] result = new int[c];
		for (int i=0; i<c; i++) {
			result[i] = buf.getInt(12 + 4*i);
		}
		return result;
	}

	private int payloadOffset() {
		int offset = 12 + 4 * csrcCount();
		if (extension()) {
			int len = buf.getUnsignedShort(offset+2);
			offset += 4 + len;
		}
		return offset;
	}

	public ByteBuf payload() {
		int off = payloadOffset();
		return buf.slice(off, buf.readableBytes() - off);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("version=").append(version())
		 .append(" padding=").append(padding())
		 .append(" extension=").append(extension())
		 .append(" csrcCount=").append(csrcCount())
		 .append(" mark=").append(mark())
		 .append(" payloadType=").append(payloadType())
		 .append(" sequenceNumber=").append(sequenceNumber())
		 .append(" timestamp=").append(timestamp())
		 .append(" ssrc=").append(ssrc());
		if (csrcCount() > 0) {
			b.append("csrcs=")
			 .append(Arrays.toString(csrcs()));
		}
		return b.toString();
	}
}