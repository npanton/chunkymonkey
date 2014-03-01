package uk.co.badgersinfoil.chunkymonkey.h264;

import org.jcodec.codecs.h264.H264Utils;

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
		return H264Utils.golomb2Signed(readUE());
	}
}
