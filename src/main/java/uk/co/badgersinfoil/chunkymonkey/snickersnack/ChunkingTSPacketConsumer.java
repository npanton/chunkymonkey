package uk.co.badgersinfoil.chunkymonkey.snickersnack;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import uk.co.badgersinfoil.chunkymonkey.ts.TSContext;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacketConsumer;

public class ChunkingTSPacketConsumer implements TSPacketConsumer {

	// TODO: move to a 'context' object
	private FileChannel file;
	private String chunkId;
	// composite which collects the buffers for a number of adjacent
	// transport stream packets, so that we can efficiently write them
	// all out in one go, rather than inefficiently giving the OS lots
	// of individual 188-byte writes to handle
	private CompositeByteBuf pending = Unpooled.compositeBuffer();
	private static final int COALESCE_THRESHOLD = 4 * 1024;  // 4KB
	private String chunkDir;

	public ChunkingTSPacketConsumer(String chunkDir) {
		this.chunkDir = chunkDir;
	}

	@Override
	public void packet(TSContext ctx, TSPacket packet) {
		try {
			ByteBuf buf = packet.getBuffer();
			if (pending.readableBytes() + buf.readableBytes() > COALESCE_THRESHOLD) {
				writeout();
			}
			pending.writeBytes(buf);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private FileChannel getChunkChannel() throws IOException {
		if (file == null) {
			Path path = FileSystems.getDefault().getPath(chunkDir, getChunkId()+".ts");
			file = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		}
		return file;
	}

	@Override
	public void end(TSContext context) {
		if (pending.readableBytes() > 0) {
			try {
				writeout();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		if (file != null) {
			close();
		}
	}

	private void writeout() throws IOException {
		pending.getBytes(0, getChunkChannel(), pending.readableBytes());
		pending.clear();
		pending.discardReadBytes();
	}

	private void close() {
		try {
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		file = null;
	}

	public String getChunkId() {
		return chunkId==null ? "0-drop" : chunkId;
	}

	public void setChunkId(String chunkId) {
		this.chunkId = chunkId;
		if (file != null) {
			if (pending.readableBytes() > 0) {
				try {
					writeout();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			close();
		}
	}

	@Override
	public TSContext createContext(TSContext parent) {
		// TODO Auto-generated method stub
		return null;
	}
}
