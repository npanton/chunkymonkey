package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiTSPacketConsumer implements TSPacketConsumer {
	
	List<TSPacketConsumer> list = new ArrayList<>();
	
	public MultiTSPacketConsumer(TSPacketConsumer... consumers) {
		Collections.addAll(list, consumers);
	}

	@Override
	public void packet(TSContext ctx, TSPacket packet) {
		for (TSPacketConsumer c : list) {
			c.packet(ctx, packet);
		}
	}

	@Override
	public void end(TSContext ctx) {
		for (TSPacketConsumer c : list) {
			c.end(ctx);
		}
	}
}
