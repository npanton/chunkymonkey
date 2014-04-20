package uk.co.badgersinfoil.chunkymonkey.ts;


public interface TSPacketConsumer {
	public class NullContext implements TSContext { }

	TSPacketConsumer NULL = new TSPacketConsumer() {
		@Override
		public void packet(TSContext ctx, TSPacket packet) { }
		@Override
		public void end(TSContext context) { }
		@Override
		public TSContext createContext(TSContext parent) { return new NullContext(); }
	};

	void packet(TSContext ctx, TSPacket packet);

	/**
	 * Called when the transport stream ends
	 */
	void end(TSContext context);

	TSContext createContext(TSContext parent);
}
