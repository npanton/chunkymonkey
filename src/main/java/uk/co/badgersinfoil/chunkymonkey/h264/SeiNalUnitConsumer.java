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
	public void unit(H264Context ctx, NALUnit u) {
		ByteBuf buf = u.getContent();
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
