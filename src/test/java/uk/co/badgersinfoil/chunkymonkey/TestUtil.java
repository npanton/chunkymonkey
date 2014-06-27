package uk.co.badgersinfoil.chunkymonkey;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class TestUtil {
	public static ByteBuf hexToBuf(String hex) {
		try {
			return Unpooled.wrappedBuffer(Hex.decodeHex(hex.replace(" ", "").toCharArray()));
		} catch (DecoderException e) {
			throw new RuntimeException(e);
		}
	}

	private TestUtil() { }
}
