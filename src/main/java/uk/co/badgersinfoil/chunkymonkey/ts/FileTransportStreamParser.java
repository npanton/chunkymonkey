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
		long size = ch.size();
		long offset=0, mapped=0;
		long packetNo = 0;
		TSContext fileContext = new TSContext() {  /* TODO: anything? */  };
		TSContext ctx = consumer.createContext(fileContext);
		while (true) {
			final long remaining = size - offset;
			if (remaining == 0) {
				break;
			}
			if (remaining > Integer.MAX_VALUE) {
				mapped = Integer.MAX_VALUE - Integer.MAX_VALUE % TSPacket.TS_PACKET_LENGTH;
			} else {
				mapped = remaining;
			}
			MappedByteBuffer in = ch.map(FileChannel.MapMode.READ_ONLY, offset, mapped);
			ByteBuf buf = Unpooled.wrappedBuffer(in);

			long packetCount = mapped / TSPacket.TS_PACKET_LENGTH;
			for (int i=0; i<packetCount ; i++) {
				ByteBuf pk = buf.slice(i*TSPacket.TS_PACKET_LENGTH, TSPacket.TS_PACKET_LENGTH);
				TSPacket packet = new TSPacket(locator, packetNo++, pk);
				if (!packet.synced()) {
					// TODO: better diagnostics.  re-sync?
					throw new RuntimeException("Transport stream synchronisation lost @packet#"+i+" in "+locator);
				}
				consumer.packet(ctx, packet);
			}
			offset += mapped;
		}
	}
}
