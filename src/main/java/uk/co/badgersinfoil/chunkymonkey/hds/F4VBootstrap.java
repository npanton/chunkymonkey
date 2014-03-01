package uk.co.badgersinfoil.chunkymonkey.hds;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;

public class F4VBootstrap {
	public enum Profile {
		NAMED_ACCESS, RANGE_ACCESS, RESERVED0, RESERVED1;

		public static Profile forId(int id) {
			switch (id) {
			case 0: return NAMED_ACCESS;
			case 1: return RANGE_ACCESS;
			case 2: return RESERVED0;
			case 3: return RESERVED1;
			default: throw new IllegalArgumentException("invalid id "+id);
			}
		}
		
	}

	private ByteBuf buf;

	public F4VBootstrap(ByteBuf buf) {
		this.buf = buf;
	}
	
	public long bootstrapInfoVersion() {
		return buf.getUnsignedInt(0);
	}
	
	public Profile profile() {
		return Profile.forId(buf.getUnsignedByte(4) >> 6);
	}
	
	public boolean live() {
		return 0 != (buf.getByte(4) & 0b00100000);
	}
	public boolean update() {
		return 0 != (buf.getByte(4) & 0b00010000);
	}
	public int reserved() {
		return buf.getByte(4) & 0b00001111;
	}
	public long timeScale() {
		return buf.getUnsignedInt(5);
	}
	public long currentMediaTimeL() {
		return buf.getLong(9);
	}
	public long smpteTimeCodeOffsetL() {
		return buf.getLong(17);
	}
	public String movieIdentifier() {
		int p = buf.bytesBefore(25, buf.capacity()-25, (byte)0x00);
		return buf.toString(25, p, Charset.forName("UTF-8"));
	}
	
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("bootstrapInfoVersion=").append(bootstrapInfoVersion())
		 .append(" profile").append(profile())
		 .append(" live=").append(live())
		 .append(" update=").append(update())
		 .append(" reserved=").append(reserved())
		 .append(" timeScale=").append(timeScale())
		 .append(" currentMediaTime=").append(currentMediaTimeL())
		 .append(" smpteTimeCodeOffset=").append(smpteTimeCodeOffsetL())
		 .append(" movieIdentifier").append(movieIdentifier());
		return b.toString();
	}
}
