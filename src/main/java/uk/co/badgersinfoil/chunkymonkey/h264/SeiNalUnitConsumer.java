package uk.co.badgersinfoil.chunkymonkey.h264;

import java.util.HashMap;
import java.util.Map;
import io.netty.buffer.ByteBuf;

public class SeiNalUnitConsumer implements NalUnitConsumer {
	
	private Map<Integer, SeiHeaderConsumer> seiConsumers = new HashMap<>();
	private SeiHeaderConsumer defaultSeiConsumer = SeiHeaderConsumer.NULL;
	
	public SeiNalUnitConsumer(Map<Integer, SeiHeaderConsumer> seiConsumers) {
		this.seiConsumers.putAll(seiConsumers);
	}

	private SeiHeaderConsumer getSeiConsumerForType(int type) {
		SeiHeaderConsumer consumer = seiConsumers.get(type);
		if (consumer == null) {
			return defaultSeiConsumer;
		}
		return consumer;
	}

	@Override
	public void start(H264Context ctx, NALUnit u) {
	}
	@Override
	public void data(H264Context ctx, ByteBuf buf, int offset, int length) {
		// we could attempt to push-parse the SEI header list, but
		// I assume SEI headers are not a significant amount of data,
		// and take the lazy approach of just buffering the entire
		// SEI NAL unit and then parse the headers at the end.
		ctx.seiBuffer().writeBytes(buf, offset, length);
	}
	@Override
	public void end(H264Context ctx) {
		ByteBuf buf = ctx.seiBuffer();
		int left;
		while ((left = buf.readableBytes()) > 0) {
			// TODO: not sure how best to identify that we need to
			// parse 'rbsp_trailing_bits', other than by assuming
			// they will be in the last byte, as implemented here,
			if (left == 1) {
				rbspTrailingBits(buf);
				break;
			}
			int type = readVar(buf);
			int size = readVar(buf);
			getSeiConsumerForType(type).header(ctx, new SeiHeader(type, buf.slice(buf.readerIndex(), size)));
			buf.skipBytes(size);
		}
		buf.clear();
	}

	@Override
	public void continuityError(H264Context ctx) {
		ByteBuf buf = ctx.seiBuffer();
		buf.clear();
	}

	private void rbspTrailingBits(ByteBuf buf) {
		short trailing = buf.readUnsignedByte();
		if (trailing != 0b10000000) {
			// TODO: proper reporting
			System.err.println("bad rbsp_trailing_bits: 0x"+Integer.toHexString(trailing));
		}
	}

	private int readVar(ByteBuf buf) {
		int val = 0;
		int b;
		do {
			b = buf.readUnsignedByte();
			val += b;
		} while (b == 0xff);
		
		return val;
	}
}
