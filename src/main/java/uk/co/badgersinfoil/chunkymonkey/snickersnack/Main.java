package uk.co.badgersinfoil.chunkymonkey.snickersnack;

import io.airlift.command.Command;
import io.airlift.command.HelpOption;
import io.airlift.command.Option;
import io.airlift.command.SingleCommand;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import javax.inject.Inject;
import uk.co.badgersinfoil.chunkymonkey.URILocator;
import uk.co.badgersinfoil.chunkymonkey.ts.FileTransportStreamParser;
import uk.co.badgersinfoil.chunkymonkey.ts.MultiTSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.MulticastReciever;
import uk.co.badgersinfoil.chunkymonkey.ts.RtpTransportStreamParser;

/**
 * One, two! One, two! and through and through <br />
 * The vorpal blade went snicker-snack!
 */
@Command(name = "snickersnack", description = "SEI-based transport stream segmenter")
public class Main {

	@Inject
	public HelpOption helpOption;

	@Option(name = { "--benchmark" }, description = "name of file on which to perform a benchmark run")
	public String benchmarkFile;

	@Option(name = { "--multicast-group" })
	public String multicastGroup;

	public static void main(String[] args) throws IOException, URISyntaxException {
		Main m = SingleCommand.singleCommand(Main.class).parse(args);
		if (m.helpOption.showHelpIfRequested()) {
			return;
		}
		m.run();
	}

	private void run() throws IOException, URISyntaxException {
		AppBuilder b = new AppBuilder();
		MultiTSPacketConsumer consumer = b.createConsumer();
		if (benchmarkFile != null) {			
			benchmark(benchmarkFile, consumer);
		} else if (multicastGroup != null) {
			// TODO: add options to configure network interface,
			NetworkInterface iface = NetworkInterface.getByIndex(0);
			MulticastReciever recieve = new MulticastReciever(multicastGroup, iface);
			RtpTransportStreamParser p = new RtpTransportStreamParser(consumer);
			recieve.recieve(p);
		} else {
			System.err.println("One of --multicast-group or --benchmark must be specified");
		}
	}

	private static void benchmark(String file,
			MultiTSPacketConsumer consumer)
			throws FileNotFoundException, IOException,
			URISyntaxException {
		File f = new File(file);
		RandomAccessFile in = new RandomAccessFile(f, "r");
		long size = f.length();
		FileTransportStreamParser parser = new FileTransportStreamParser(consumer);
		long start = System.currentTimeMillis();
		parser.parse(new URILocator(new URI(file)), in);
		in.close();
		long time = System.currentTimeMillis() - start;
		long rate = size*1000/time;
		System.out.println(new Date()+" finished - took "+(time)+" milliseconds, ("+(rate/1024/1024)+"Mbyte/s, "+(rate*8/1024/1024)+"Mbit/s)");
	}

}
