package uk.co.badgersinfoil.chunkymonkey.ts;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;


public interface TSPacketConsumer {
	public class NullContext implements MediaContext { }

	TSPacketConsumer NULL = new TSPacketConsumer() {
		@Override
		public void packet(MediaContext ctx, TSPacket packet) { }
		@Override
		public void end(MediaContext context) { }
		@Override
		public MediaContext createContext(MediaContext parent) { return new NullContext(); }
	};

	void packet(MediaContext ctx, TSPacket packet);

	/**
	 * Called when the transport stream ends
	 */
	void end(MediaContext context);

	MediaContext createContext(MediaContext parent);
}
