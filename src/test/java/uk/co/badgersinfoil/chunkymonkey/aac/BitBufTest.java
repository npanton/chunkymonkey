package uk.co.badgersinfoil.chunkymonkey.aac;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class BitBufTest {
	@Test
	public void basic() {
		ByteBuf bytes = Unpooled.wrappedBuffer(new byte[] {0b00000010, 0b01000000});
		BitBuf b = new BitBuf(bytes);
		assertEquals(16, b.readableBits());
		assertEquals(0, b.readBits(6));
		assertEquals(10, b.readableBits());
		assertEquals(0b1001, b.readBits(4));
		assertEquals(6, b.readableBits());
		assertEquals(0, b.readBits(6));
		assertEquals(0, b.readableBits());
	}

	@Test
	public void multibyte() {
		ByteBuf bytes = Unpooled.wrappedBuffer(new byte[] {0b00000010, 0b01000000});
		BitBuf b = new BitBuf(bytes);
		assertEquals(0b0000001001000000, b.readBits(16));
	}

	@Test
	public void signBit() {
		ByteBuf bytes = Unpooled.wrappedBuffer(new byte[] {(byte)0b11111111});
		BitBuf b = new BitBuf(bytes);
		assertEquals(1, b.readBits(1));
	}
	@Test
	public void peekOnBoundry() {
		ByteBuf bytes = Unpooled.wrappedBuffer(new byte[] {(byte)0b11111111});
		BitBuf b = new BitBuf(bytes);
		b.peek(8);
		// should not raise an exception
	}
}
