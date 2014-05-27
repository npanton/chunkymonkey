package uk.co.badgersinfoil.chunkymonkey.snickersnack;

import io.airlift.command.Command;
import io.airlift.command.HelpOption;
import io.airlift.command.Option;
import io.airlift.command.SingleCommand;
import io.netty.util.ResourceLeakDetector;
import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Enumeration;
import javax.inject.Inject;
import uk.co.badgersinfoil.chunkymonkey.ConsoleReporter;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.rtp.MulticastReceiver;
import uk.co.badgersinfoil.chunkymonkey.rtp.RTPErrorHandler;
import uk.co.badgersinfoil.chunkymonkey.rtp.RtpParser;
import uk.co.badgersinfoil.chunkymonkey.rtp.MulticastReceiver.MulticastReceiverContext;
import uk.co.badgersinfoil.chunkymonkey.ts.FileTransportStreamParser;
import uk.co.badgersinfoil.chunkymonkey.ts.MultiTSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.RtpTransportStreamParser;

/**
 * One, two! One, two! and through and through <br />
 * The vorpal blade went snicker-snack!
 */
@Command(name = "snickersnack", description = "SEI-based transport stream segmenter")
public class Main {

	private final class ReportingRTPErrorHandler implements RTPErrorHandler {
		private Reporter rep;

		public ReportingRTPErrorHandler(Reporter rep) {
			this.rep = rep;
		}

		@Override
		public void unexpectedSsrc(Locator loc, long expectedSsrc, long actualSsrc) {
			rep.carp(loc, "RTP unexpected SSRC 0x%08x (expecting 0x%08x)", actualSsrc, expectedSsrc);
		}

		@Override
		public void unexpectedSequenceNumber(Locator loc, int expectedSeq, int actualSeq) {
			rep.carp(loc, "RTP unexpected sequence number %d (expecting %d)", actualSeq, expectedSeq);
		}

		@Override
		public void timestampJumped(Locator loc, long lastTimestamp, long timestamp) {
			rep.carp(loc, "RTP unexpected timestamp jump from %d to %d", lastTimestamp, timestamp);
		}

		@Override
		public void timeWentBackwards(Locator loc, long lastTimestamp, long timestamp) {
			rep.carp(loc, "RTP timestamp went backwards from %d to %d", lastTimestamp, timestamp);
		}
	}

	@Inject
	public HelpOption helpOption;

	@Option(name = { "--benchmark" }, description = "name of file on which to perform a benchmark run")
	public String benchmarkFile;

	@Option(name = { "--multicast-group" })
	public String multicastGroup;

	@Option(name = { "--chunk-dir" }, description = "Destination into which transport stream chunks are to be written", required=true )
	public String chunkDir;

	@Option(name = { "--interface" }, description = "Name of the network interface on which to join the multicast group" )
	public String ifname;

	public static void main(String[] args) throws IOException, URISyntaxException {
		Main m = SingleCommand.singleCommand(Main.class).parse(args);
		if (m.helpOption.showHelpIfRequested()) {
			return;
		}
		m.run();
	}

	private void run() throws IOException, URISyntaxException {
		ResourceLeakDetector.setEnabled(false);

		Reporter rep = new ConsoleReporter();
		AppBuilder b = new AppBuilder();
		b.chunkDir(chunkDir);
		MultiTSPacketConsumer consumer = b.createConsumer(rep);
		if (benchmarkFile != null) {
			benchmark(benchmarkFile, consumer);
		} else if (multicastGroup != null) {
			NetworkInterface iface = getInterface();
			RtpTransportStreamParser p = new RtpTransportStreamParser(consumer);
			RtpParser rtpParse = new RtpParser(p);
			MulticastReceiver receive = new MulticastReceiver(rtpParse, multicastGroup, iface);
			rtpParse.setRTPErrorHandler(new ReportingRTPErrorHandler(rep));
			MulticastReceiverContext ctx = receive.createContext();
			receive.receive(ctx);
		} else {
			System.err.println("One of --multicast-group or --benchmark must be specified");
		}
	}

	private NetworkInterface getInterface() throws SocketException {
		NetworkInterface iface;
		if (ifname == null) {
			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
			if (!e.hasMoreElements() || (iface = e.nextElement()) == null) {
				throw new RuntimeException("No network interfaces?");
			}
			System.err.println("Listening on "+iface.getDisplayName());
		} else {
			iface = NetworkInterface.getByName(ifname);
			if (iface == null) {
				throw new RuntimeException("No such network interface: "+ifname);
			}
		}
		return iface;
	}

	private static void benchmark(String file,
	                              MultiTSPacketConsumer consumer)
		throws IOException
	{
		File f = new File(file);
		long size = f.length();
		FileTransportStreamParser parser = new FileTransportStreamParser(consumer);
		FileTransportStreamParser.FileTsContext ctx = parser.createContext();
		long start = System.currentTimeMillis();
		parser.parse(ctx, f);
		long time = System.currentTimeMillis() - start;
		long rate = size*1000/time;
		System.out.println(new Date()+" finished - took "+(time)+" milliseconds, ("+(rate/1024/1024)+"Mbyte/s, "+(rate*8/1024/1024)+"Mbit/s)");
	}

}
