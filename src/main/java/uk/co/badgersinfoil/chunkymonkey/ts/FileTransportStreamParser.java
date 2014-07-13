package uk.co.badgersinfoil.chunkymonkey.ts;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.event.URILocator;

public class FileTransportStreamParser {

	public static class FileContext implements MediaContext {

		private MediaContext ctx;
		public File currentFile;
		public long packetNo;

		public void setConsumerContext(MediaContext ctx) {
			this.ctx = ctx;
		}
		public MediaContext getConsumerContext() {
			return ctx;
		}

		@Override
		public Locator getLocator() {
			return new TSPacketLocator(new URILocator(currentFile.toURI()), packetNo);
		}
	}

	private TSPacketConsumer consumer;

	public FileTransportStreamParser(TSPacketConsumer consumer) {
		this.consumer = consumer;
	}

	public void parse(FileContext fctx, File f) throws IOException {
		fctx.currentFile = f;
		RandomAccessFile file = new RandomAccessFile(f, "r");
		try {
			FileChannel ch = file.getChannel();
			long size = ch.size();
			long offset=0, mapped=0;
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
				MediaContext ctx = fctx.getConsumerContext();
				for (int i=0; i<packetCount ; i++) {
					fctx.packetNo++;
					ByteBuf pk = buf.slice(i*TSPacket.TS_PACKET_LENGTH, TSPacket.TS_PACKET_LENGTH);
					TSPacket packet = new TSPacket(pk);
					if (!packet.synced()) {
						// TODO: better diagnostics.  re-sync?
						throw new RuntimeException("Transport stream synchronisation lost at "+fctx.getLocator());
					}
					consumer.packet(ctx, packet);
				}
				offset += mapped;
			}
		} finally {
			file.close();
		}
	}

	public FileContext createContext() {
		FileContext fctx = new FileContext();
		MediaContext ctx = consumer.createContext(fctx);
		fctx.setConsumerContext(ctx);
		return fctx;
	}
}
