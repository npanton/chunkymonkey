package uk.co.badgersinfoil.chunkymonkey.ts;


public interface TSPacketConsumer {
	TSPacketConsumer NULL = new TSPacketConsumer() {
		@Override
		public void packet(TSContext ctx, TSPacket packet) { }
		@Override
		public void end(TSContext context) { }
	};

	void packet(TSContext ctx, TSPacket packet);

	/**
	 * Called when the transport stream ends
	 */
	void end(TSContext context);
}
