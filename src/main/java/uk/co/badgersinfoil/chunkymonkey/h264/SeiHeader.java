package uk.co.badgersinfoil.chunkymonkey.h264;

import io.netty.buffer.ByteBuf;

public class SeiHeader {

	public static final int PIC_TIMING = 1;

	private int type;
	private ByteBuf buf;

	public SeiHeader(int type, ByteBuf buf) {
		this.type = type;
		this.buf = buf;
	}
	
	public ByteBuf getBuf() {
		return buf;
	}
}
