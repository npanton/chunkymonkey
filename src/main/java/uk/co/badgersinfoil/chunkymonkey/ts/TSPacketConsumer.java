package uk.co.badgersinfoil.chunkymonkey.ts;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;


public interface TSPacketConsumer {
	public class NullContext implements MediaContext {
		@Override
		public Locator getLocator() { return null; }
	}

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
