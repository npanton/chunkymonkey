package uk.co.badgersinfoil.chunkymonkey.h264;

import io.netty.buffer.ByteBuf;
import uk.co.badgersinfoil.chunkymonkey.aac.BitBuf;

public class H264BitBuf extends BitBuf {

	public H264BitBuf(ByteBuf buf) {
		super(buf);
	}

	public int readUE() {
		int cnt = 0;
		while (readBits(1) == 0 && cnt < 31) {
			cnt++;
		}
		int res = 0;
		if (cnt > 0) {
			long val = readBits(cnt);
			res = (int)((1 << cnt) - 1 + val);
		}
		return res;
	}
	public int readSE() {
		return golomb2Signed(readUE());
	}

	private static int golomb2Signed(int val) {
		int sign = ((val & 0x1) << 1) - 1;
		return ((val >> 1) + (val & 0x1)) * sign;
	}

	public boolean moreRbspData() {
		return readableBits() > 8
			|| readableBits() > 0 && peek(readableBits()) != predictRbspTrailingBits();
	}

	private int predictRbspTrailingBits() {
		return 1 << (readableBits()-1);
	}
}
