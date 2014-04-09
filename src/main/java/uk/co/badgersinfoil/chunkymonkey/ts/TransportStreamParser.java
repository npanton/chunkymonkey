package uk.co.badgersinfoil.chunkymonkey.ts;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.io.InputStream;

import uk.co.badgersinfoil.chunkymonkey.Locator;


public class TransportStreamParser {

	private TSPacketConsumer consumer;
	private Locator locator;
	private int packetNo;

	public TransportStreamParser(Locator locator, TSPacketConsumer consumer) {
		this.locator = locator;
		this.consumer = consumer;
	}

	public long getPacketNo() {
		return packetNo;
	}

	public Locator getLocator() {
		return locator;
	}

	public void parse(InputStream stream) throws IOException {
		while (true) {
			ByteBuf buf = Unpooled.buffer();
			if (!readPacket(stream, buf, TSPacket.TS_PACKET_LENGTH)) {
				return;
			}
			ByteBuf pk = buf.slice(buf.readerIndex(), TSPacket.TS_PACKET_LENGTH);
			TSPacket packet = new TSPacket(locator, packetNo, pk);
			if (!packet.synced()) {
				// TODO: better diagnostics.  re-sync?
				throw new RuntimeException("Transport stream synchronisation lost @packet#"+packetNo+" in "+locator);
			}
			consumer.packet(null, packet);
			//if (packet.adaptionControl().adaptionFieldPresent() && packet.getAdaptionField().pcrFlag()) {
			//	System.out.println(packet.getAdaptionField().pcr());
			//}
			packetNo++;
		}
	}

	private boolean readPacket(InputStream in, ByteBuf buf, int packetSize) throws IOException {
		int read = 0;
		while (read < packetSize) {
			int r = buf.writeBytes(in, packetSize - read);
			if (r == -1) {
				if (read > 0 && read < packetSize) {
					throw new RuntimeException("Wanted "+packetSize+" bytes, but only "+read+" read");
				}
				return false;
			}
			read += r;
		}
		return true;
	}
}
