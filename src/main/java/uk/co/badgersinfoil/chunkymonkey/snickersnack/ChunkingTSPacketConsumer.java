package uk.co.badgersinfoil.chunkymonkey.snickersnack;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
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
	private AtomicReference<ByteBuf> pending = new AtomicReference<>(Unpooled.buffer());
	private static final int COALESCE_THRESHOLD = 1024 * 1024;  // 1MB
	private static final int MAX_BUFFER = 10 * 1024 * 1024;  // 10MB
	private String chunkDir;
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	private Future<Void> lastWrite;

	public ChunkingTSPacketConsumer(String chunkDir) {
		this.chunkDir = chunkDir;
	}

	@Override
	public void packet(TSContext ctx, TSPacket packet) {
		if (pending.get().readableBytes() > MAX_BUFFER) {
			System.err.println("Writeout too slow, dropping a packet");
		}
		ByteBuf buf = packet.getBuffer();
		if (pending.get().readableBytes() + buf.readableBytes() > COALESCE_THRESHOLD) {
			try {
				writeout();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		pending.get().writeBytes(buf);
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
		if (pending.get().readableBytes() > 0) {
			try {
				writeout();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (file != null) {
			close();
		}
	}


	private void writeout() throws IOException {
		if (lastWrite != null && !lastWrite.isDone()) {
			return;
		}
		final ByteBuf buf = pending.get();
		final FileChannel out = getChunkChannel();
		lastWrite = executor.submit(new Callable<Void>() {
			@Override
			public Void call() {
				try {
					buf.readBytes(out, buf.readableBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}
		});
		pending.set(Unpooled.buffer());
	}

	private void close() {
		final FileChannel file = this.file;
		this.file = null;
		executor.submit(new Callable<Void>() {
			@Override
			public Void call() {
				try {
					file.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}
		});
	}

	public String getChunkId() {
		return chunkId==null ? "0-drop" : chunkId;
	}

	public void setChunkId(String chunkId) {
		this.chunkId = chunkId;
		if (file != null) {
			if (pending.get().readableBytes() > 0) {
				try {
					writeout();
				} catch (IOException e) {
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
