package uk.co.badgersinfoil.chunkymonkey.conformist;

import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.HelpOption;
import io.airlift.command.Option;
import io.airlift.command.SingleCommand;
import io.netty.util.ResourceLeakDetector;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.eclipse.jetty.server.Server;
import uk.co.badgersinfoil.chunkymonkey.AnsiConsoleReporter;
import uk.co.badgersinfoil.chunkymonkey.ConsoleReporter;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.conformist.api.ServerBuilder;
import uk.co.badgersinfoil.chunkymonkey.conformist.redundancy.HlsRedundantStreamContext;
import uk.co.badgersinfoil.chunkymonkey.conformist.redundancy.HlsRedundantStreamProcessor;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMasterPlaylistContext;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMasterPlaylistProcessor;
import uk.co.badgersinfoil.chunkymonkey.ts.FileTransportStreamParser;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacketConsumer;

@Command(name = "conformist", description = "Media stream checker")
public class Main {

	@Inject
	public HelpOption helpOption;

	@Option(name = { "--user-agent" }, description = "User-Agent header value to send in HTTP requests.")
	public String userAgent;

	@Option(name = { "--time-limit" }, description = "Stop consuming the stream after this many seconds from startup" )
	public Integer timeLimit;

	@Option(name = { "--colour" }, description = "Use ANSI colour sequences in output logging")
	public boolean colour = false;

	@Arguments(description="URL of the stream to check", required=true)
	List<String> urls;

	public static void main(String[] args) throws Exception {
		Main m = SingleCommand.singleCommand(Main.class).parse(args);
		if (m.helpOption.showHelpIfRequested()) {
			return;
		}
		m.run();
	}

	private void run() throws Exception {
		ResourceLeakDetector.setEnabled(false);

		AppBuilder b = new AppBuilder();
		if (userAgent != null) {
			b.setUserAgent(userAgent);
		}
		ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(16);
		Reporter rep = colour ? new AnsiConsoleReporter() : new ConsoleReporter();
		if (urls.size() == 1) {
			URI uri = new URI(urls.get(0));
			if ("http".equals(uri.getScheme())) {
				// assume HTTP means HLS,
				b.hls(true);
				HlsMasterPlaylistProcessor processor = b.buildSingle(scheduledExecutor, rep);
				HlsMasterPlaylistContext ctx = processor.createContext(uri);
				Server server = ServerBuilder.create(ctx).build();
				processor.start(ctx);
				server.start();
				if (timeLimit == null) {
					while (true) {
						Thread.sleep(10_000);
					}
				} else {
					Thread.sleep(timeLimit * 1000);
				}
				System.err.println("Shutting down HLS processing");
				processor.stop(ctx);
				scheduledExecutor.shutdown();
				scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS);
			} else if (uri.isAbsolute()) {
				System.err.println("Only http (HLS) or local files (TS) supported: "+uri);
				System.exit(-1);
			} else {
				b.hls(false);
				TSPacketConsumer consumer = b.createConsumer(rep);
				FileTransportStreamParser parser = new FileTransportStreamParser(consumer);
				FileTransportStreamParser.FileContext ctx = parser.createContext();
				parser.parse(ctx, new File(uri.getPath()));
				return;
			}
		} else if (urls.size() == 2) {
			HlsRedundantStreamProcessor processor = b.buildRedundant(scheduledExecutor, rep);
			URI uri1 = new URI(urls.get(0));
			URI uri2 = new URI(urls.get(1));
			HlsRedundantStreamContext ctx = processor.createContext(uri1, uri2);
			processor.start(ctx);
		} else {
			System.err.println("wrong number of arguments, "+urls.size()+", expected 1 or 2.");
		}
	}
}
