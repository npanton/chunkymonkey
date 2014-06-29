package uk.co.badgersinfoil.chunkymonkey.adts;

import uk.co.badgersinfoil.chunkymonkey.aac.AacProfile;
import uk.co.badgersinfoil.chunkymonkey.aac.ChannelConfiguration;
import uk.co.badgersinfoil.chunkymonkey.aac.SamplingFrequencyIndex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ADTSFrame {

	private static final int HEADER_MIN_LENGTH = 7;
	private static final int HEADER_CRC_LENGTH = 9;

	public static enum MpegVersion {
		MPEG4, MPEG2
	}

	private ByteBuf buf;

	public ADTSFrame(ByteBuf buf) {
		this.buf = buf;
	}
	public void append(ByteBuf buf) {
		this.buf = Unpooled.wrappedBuffer(this.buf, buf);
	}
	public int syncWord() {
		return buf.getUnsignedByte(0) << 4 | (buf.getUnsignedByte(1) & 0b11110000) >> 4;
	}
	public MpegVersion mpegVersion() {
		int v = (buf.getByte(1) & 0b00001000) >> 3;
		switch (v) {
		    case 0:
			return MpegVersion.MPEG4;
		    case 1:
			return MpegVersion.MPEG2;
		    default:
			throw new Error("Imposible value: "+v);
		}
	}
	public int layer() {
		return (buf.getByte(1) & 0b00000110) >> 1;
	}
	public boolean crcPresent() {
		// 0=>Present, 1=>Absent
		return 0 == (buf.getByte(1) & 0b00000001);
	}
	public AacProfile profile() {
		int p = (buf.getByte(2) & 0b11000000) >> 6;
		return AacProfile.forIndex(p);
	}
	public SamplingFrequencyIndex samplingFrequency() {
		int i = (buf.getByte(2) & 0b00111100) >> 2;
		return SamplingFrequencyIndex.forIndex(i);
	}
	public boolean privateStream() {
		return 0 != (buf.getByte(2) & 0b00000010);
	}
	public ChannelConfiguration channelConfig() {
		int c = (buf.getByte(2) & 0b00000001) << 2
		      |(buf.getByte(3) & 0b11000000) >> 6;
		return ChannelConfiguration.forIndex(c);
	}
	public int originality() {
		return (buf.getByte(3) & 0b00100000) >> 5;
	}
	public int home() {
		return (buf.getByte(3) & 0b00010000) >> 4;
	}
	public int copyrighted() {
		return (buf.getByte(3) & 0b00001000) >> 3;
	}
	public int copyrightStart() {
		return (buf.getByte(3) & 0b00000100) >> 2;
	}
	public boolean isFrameLengthInvalid() {
		return frameLength() <= payloadStart();
	}
	/**
	 * Includes the length of the frame header (7 or 9 bytes, depending on crcPresent() flag).
	 */
	public int frameLength() {
		return (buf.getByte(3) & 0b00000011) << 11
		      |((buf.getByte(4) & 0b11111111) << 3)
		      |((buf.getByte(5) & 0b11100000) >> 5);
	}
	public int bufferFullness() {
		return ((buf.getByte(5) & 0b00011111) << 6)
		      |((buf.getByte(6) & 0b11111100) >> 2);
	}
	public int blockCount() {
		return buf.getByte(6) & 0b00000011;
	}
	public int payloadLength() {
		return frameLength() - payloadStart();
	}
	public boolean isHeaderComplete() {
		return buf.readableBytes() >= HEADER_CRC_LENGTH
		     ||(buf.readableBytes() >= HEADER_CRC_LENGTH && !crcPresent());
	}
	public boolean isComplete() {
		final int readable = buf.readableBytes();
		return readable > HEADER_MIN_LENGTH && readable >= frameLength();
	}
	private void assertComplete() {
		if (!isComplete()) {
			throw new RuntimeException("Frame is not yet complete");
		}
	}
	public int payloadStart() {
		return crcPresent() ? HEADER_CRC_LENGTH : HEADER_MIN_LENGTH;
	}
	/**
	 * @throws IllegalStateException if the length field value is smaller
	 *         than the size of this frames headers
	 */
	public ByteBuf payload() {
		assertComplete();
		int offset = payloadStart();
		if (frameLength() <= offset) {
			throw new IllegalStateException("frame length ("+frameLength()+") <= paload start ("+offset+")");
		}
		return buf.slice(offset, payloadLength());
	}
	/**
	 * @throws IllegalStateException if the length field value is smaller
	 *         than the size of this frames headers
	 */
	public ByteBuf trailingData() {
		assertComplete();
		int offset = frameLength();
		if (offset <= payloadStart()) {
			throw new IllegalStateException("frame length ("+offset+") <= paload start ("+payloadStart()+")");
		}
		if (offset == buf.readableBytes()) {
			return null;
		}
		return buf.slice(offset, buf.readableBytes() - offset);
	}

	@Override
	public String toString() {
		if (!isHeaderComplete()) {
			return "Incomplete ADTS headers (only "+buf.readableBytes()+" bytes present)";
		}
		StringBuilder b = new StringBuilder();
//		b.append("[complete:").append(isComplete()).append(",readable=").append(buf.readableBytes());
//		if (isComplete()) b.append(",payloadStart=").append(payloadStart());
//		b.append("] ");
		b.append("syncWord=0x").append(Integer.toHexString(syncWord()))
		 .append(" mpegVersion=").append(mpegVersion())
		 .append(" layer=").append(layer())
		 .append(" crcPresent=").append(crcPresent())
		 .append(" profile=").append(profile())
		 .append(" samplingFrequency=").append(samplingFrequency())
		 .append(" privateStream=").append(privateStream())
		 .append(" channelConfig=").append(channelConfig())
		 .append(" originality=").append(originality())
		 .append(" home=").append(home())
		 .append(" copyrighted=").append(copyrighted())
		 .append(" copyrightStart=").append(copyrightStart())
		 .append(" frameLength=").append(frameLength())
		 .append(" bufferFullness=").append(bufferFullness())
		 .append(" blockCount=").append(blockCount());
//		if (isComplete()) {
//			b.append(" payload=").append(ByteBufUtil.hexDump(payload()));
//		}
		return b.toString();
	}
}
