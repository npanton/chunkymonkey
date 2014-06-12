package uk.co.badgersinfoil.chunkymonkey.aac;

import io.netty.buffer.ByteBuf;

public class BitBuf {
	private ByteBuf buf;
	private int lastByte = 0;
	private int remainingBits = 0;

	public BitBuf(ByteBuf buf) {
		this.buf = buf;
	}

	// TODO: inefficient hack to allow > 8 bits
	public int readBits(int length) {
		if (length > 31) {
			throw new IllegalArgumentException("Length greater than 31: "+length);
		}
		int result = 0;
		while (length > 8) {
			result <<= 8;
			result |= readBits0(8);
			length -= 8;
		}
		result <<= length;
		result |= readBits0(length);
		return result;
	}

	private int readBits0(int length) {
		if (length > 8) {
			throw new IllegalArgumentException("Length greater than 8: "+length);
		}
		int resultBits = 0;
		int toRead = Math.min(length, remainingBits);
		if (toRead > 0) {
			final int mask = 0xff00 >> toRead;
			int shift = (8 - toRead);
			resultBits = (lastByte & mask) >> shift;
			lastByte = (lastByte << toRead) & 0xff;
			remainingBits -= toRead;
		}
		toRead = length - toRead;
		if (toRead > 0) {
			resultBits <<= toRead;
			lastByte = buf.readUnsignedByte();
			final int mask = 0xff00 >> toRead;
			remainingBits = 8 - toRead;
			resultBits |= (lastByte & mask) >> remainingBits;
			lastByte = (lastByte << toRead) & 0xff;
		}
		return resultBits;
	}

	public int readBit() {
		return readBits0(1);
	}

	public boolean readBool() {
		return readBits0(1) != 0;
	}

	public int readableBits() {
		return remainingBits + buf.readableBytes() * 8;
	}

	/**
	 * Allows the next few bits to be seen, but only up to the next byte
	 * boundary.
	 */
	public int peek(int n) {
		if (n > remainingBits) {
			throw new IllegalArgumentException(n+" is greater than remaining bits in the current byte, "+remainingBits);
		}
		final int mask = 0xff00 >> n;
		int shift = (8 - n);
		return (lastByte & mask) >> shift;
	}
}
