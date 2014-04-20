package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import uk.co.badgersinfoil.chunkymonkey.ConsoleReporter;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMasterPlaylistContext;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMasterPlaylistProcessor;

public class Main {

	public static void main(String[] args) throws Exception {
		URI uri = new URI(args[0]);
		AppBuilder b = new AppBuilder();
		ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(4);
		Reporter rep = new ConsoleReporter();
		HlsMasterPlaylistProcessor processor = b.build(scheduledExecutor, rep);
		HlsMasterPlaylistContext ctx = processor.createContext(uri);
		processor.start(ctx);
		// TODO: what to do with the main thread in the meantime?
		while (true) {
			Thread.sleep(10_000);
		}
		//processor.stop(ctx);
	}
}
