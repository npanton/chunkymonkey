package uk.co.badgersinfoil.chunkymonkey.ts;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import uk.co.badgersinfoil.chunkymonkey.Locator;

public class FileTransportStreamParser {

	private TSPacketConsumer consumer;

	public FileTransportStreamParser(TSPacketConsumer consumer) {
		this.consumer = consumer;
	}

	public void parse(Locator locator, RandomAccessFile file) throws IOException {
		FileChannel ch = file.getChannel();
		MappedByteBuffer in = ch.map(FileChannel.MapMode.READ_ONLY, 0, ch.size());
		ByteBuf buf = Unpooled.wrappedBuffer(in);
		
		long packetCount = ch.size() / TSPacket.TS_PACKET_LENGTH;
		for (int packetNo=0; packetNo<packetCount ; packetNo++) {
			ByteBuf pk = buf.slice(packetNo*TSPacket.TS_PACKET_LENGTH, TSPacket.TS_PACKET_LENGTH);
			TSPacket packet = new TSPacket(locator, packetNo, pk);
			if (!packet.synced()) {
				// TODO: better diagnostics.  re-sync?
				throw new RuntimeException("Transport stream synchronisation lost @packet#"+packetNo+" in "+locator);
			}
			consumer.packet(null, packet);
		}
	}
}
