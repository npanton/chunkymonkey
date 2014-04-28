package uk.co.badgersinfoil.chunkymonkey.conformist;

import io.netty.util.ResourceLeakDetector;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import uk.co.badgersinfoil.chunkymonkey.ConsoleReporter;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.conformist.redundancy.HlsRedundantStreamContext;
import uk.co.badgersinfoil.chunkymonkey.conformist.redundancy.HlsRedundantStreamProcessor;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMasterPlaylistContext;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMasterPlaylistProcessor;

public class Main {

	public static void main(String[] args) throws Exception {
		ResourceLeakDetector.setEnabled(false);

		AppBuilder b = new AppBuilder();
		ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(4);
		Reporter rep = new ConsoleReporter();
		if (args.length == 1) {
			HlsMasterPlaylistProcessor processor = b.buildSingle(scheduledExecutor, rep);
			URI uri = new URI(args[0]);
			HlsMasterPlaylistContext ctx = processor.createContext(uri);
			processor.start(ctx);
		} else if (args.length == 2) {
			HlsRedundantStreamProcessor processor = b.buildRedundant(scheduledExecutor, rep);
			URI uri1 = new URI(args[0]);
			URI uri2 = new URI(args[1]);
			HlsRedundantStreamContext ctx = processor.createContext(uri1, uri2);
			processor.start(ctx);
		} else {
			System.err.println("wrong number of arguments, "+args.length+", expected 1 or 2.");
		}
		// TODO: what to do with the main thread in the meantime?
		while (true) {
			Thread.sleep(10_000);
		}
		//processor.stop(ctx);
	}
}
