package uk.co.badgersinfoil.chunkymonkey.snickersnak;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import uk.co.badgersinfoil.chunkymonkey.ts.TSContext;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacketConsumer;

public class ChunkingTSPacketConsumer implements TSPacketConsumer {

	// TODO: move to a 'context' object
	private FileChannel file;
	private String chunkId;

	@Override
	public void packet(TSContext ctx, TSPacket packet) {
		if (chunkId == null) {
			return;
		}
		try {
			FileChannel ch = getChunkChannel();
			ByteBuf buf = packet.getBuffer();
			buf.getBytes(0, ch, buf.readableBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private FileChannel getChunkChannel() throws IOException {
		if (file == null) {
			Path path = FileSystems.getDefault().getPath("/tmp", "chunks", getChunkId()+".ts");
			file = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		}
		return file;
	}

	@Override
	public void end(TSContext context) {
		if (file != null) {
			close();
		}
	}

	private void close() {
		try {
			file.close();
			file = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getChunkId() {
		return chunkId;
	}

	public void setChunkId(String chunkId) {
		this.chunkId = chunkId;
		if (file != null) {
			close();
		}
	}
}
