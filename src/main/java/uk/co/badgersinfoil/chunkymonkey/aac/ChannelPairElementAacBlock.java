package uk.co.badgersinfoil.chunkymonkey.aac;

import io.netty.buffer.ByteBuf;

public class ChannelPairElementAacBlock extends AacBlock {
	static enum WindowSequence {
		ONLY_LONG(0), LONG_START(1), EIGHT_SHORT_SEQUENCE(2), LONG_STOP(3);
		
		private int winSeqVal;

		private WindowSequence(int winSeqVal) {
			this.winSeqVal = winSeqVal;
		}
		
		public static WindowSequence valueOf(int winSeqVal) {
			switch (winSeqVal) {
			case 0: return ONLY_LONG;
			case 1: return LONG_START;
			case 2: return EIGHT_SHORT_SEQUENCE;
			case 3: return LONG_STOP;

			// Shouldn't trigger for invalid bitstreams, as
			// the field in the bitstream is only 2 bits long,
			default: throw new IllegalArgumentException("Invalid winSeqVal "+winSeqVal);
			}
		}
	}

	private ByteBuf aacFrame;

	public ChannelPairElementAacBlock(ByteBuf aacFrame) {
		this.aacFrame = aacFrame;
	}
	
	
}
