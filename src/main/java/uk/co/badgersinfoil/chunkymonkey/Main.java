package uk.co.badgersinfoil.chunkymonkey;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.chilicat.m3u8.Element;
import net.chilicat.m3u8.PlayList;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import uk.co.badgersinfoil.chunkymonkey.aac.AacAdtsFrameConsumer;
import uk.co.badgersinfoil.chunkymonkey.adts.AdtsFrameConsumer;
import uk.co.badgersinfoil.chunkymonkey.adts.AdtsPesConsumer;
import uk.co.badgersinfoil.chunkymonkey.adts.ValidatingAdtsFrameConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.H264PesConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.NalUnitConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.SeiHeaderConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.SeiNalUnitConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.SeqParamSetNalUnitConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.NALUnit.UnitType;
import uk.co.badgersinfoil.chunkymonkey.hls.HLSContext;
import uk.co.badgersinfoil.chunkymonkey.ts.FileTransportStreamParser;
import uk.co.badgersinfoil.chunkymonkey.ts.TransportContext;
import uk.co.badgersinfoil.chunkymonkey.ts.MultiTSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PATConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PESConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PIDFilterPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PIDFilterPacketConsumer.FilterEntry;
import uk.co.badgersinfoil.chunkymonkey.ts.PesTSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.ProgramMapTable;
import uk.co.badgersinfoil.chunkymonkey.ts.StreamProcRegistry;
import uk.co.badgersinfoil.chunkymonkey.ts.StreamTSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacketValidator;
import uk.co.badgersinfoil.chunkymonkey.ts.UnhandledStreamTSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.ValidatingPesConsumer;

public class Main {

	public static void main(String[] args) throws Exception {
		hack();
		if (true) return;
		URI uri = new URI(args[0]);
		
		MultiTSPacketConsumer tsConsumer = createConsumer();
		HLSContext context = new HLSContext(uri, tsConsumer);
		CloseableHttpClient httpclient = HttpClientBuilder.create().build();
		context.setHttpClient(httpclient);
		try {
			context.consumeStream();
		} finally {
			httpclient.close();
		}
		System.out.println("Done");
	}
	
	private static Pattern seq = Pattern.compile("(\\d+)\\.ts$");

	private static void validatePlaylist(PlayList playlist) {
		long lastSeqNo = -1;
		Element lastElement = null;
		for (Element element : playlist) {
			Matcher m = seq.matcher(element.getURI().getPath());
			if (m.find()) {
				long seqNo = Long.parseLong(m.group(1));
				if (lastSeqNo != -1) {
					if (seqNo != lastSeqNo + 1) {
						System.err.println("Unexpcted sequence difference "+(seqNo-lastSeqNo)+" from "+element.getURI()+" to "+lastElement.getURI());
					}
				}
				lastSeqNo = seqNo;
			}
			lastElement = element;
		}
	}

	private static void hack() throws IOException, URISyntaxException {
//		String file = "/home/dave/Downloads/bbc_one_london-SCO-b_1391370588_1391370721.ts";
//		String file = "/home/dave/Downloads/bbc_one_hd-NCO-a_1390564899_1390565246.ts";
		String file = "/tmp/bbc_one_hd-NCO-a_1390564899_1390565246.ts";
//		String file = "/home/dave/workspace/chunkymonkey/from_kevin/BBCRD_Jan2014_iframe_progressive.ts";
//		String file = "/home/dave/workspace/chunkymonkey/from_kevin/BBCRD_Jan2014_iframe_interlaced.ts";
//		String file = "/home/dave/workspace/chunkymonkey/media/ron-part.ts";
//		String file = "/home/dave/Downloads/20131226_004000_bbconehd_anchorman_the_legend_of_ron.ts";
		File f = new File(file);
		RandomAccessFile in = new RandomAccessFile(f, "r");
		long size = f.length();
		MultiTSPacketConsumer consumer = createConsumer();
		FileTransportStreamParser parser = new FileTransportStreamParser(consumer);
		long start = System.currentTimeMillis();
		parser.parse(new URILocator(new URI(file)), in);
		in.close();
		long time = System.currentTimeMillis() - start;
		long rate = size*1000/time;
		System.out.println(new Date()+" finished - took "+(time)+" milliseconds, ("+(rate/1024/1024)+"Mbyte/s, "+(rate*8/1024/1024)+"Mbit/s)");
	}

	private static MultiTSPacketConsumer createConsumer() {
		Reporter rep = new ConsoleReporter();
		PIDFilterPacketConsumer pidFilter = new PIDFilterPacketConsumer(rep);
		Map<Integer, StreamTSPacketConsumer> map = new HashMap<>();
		map.put(ProgramMapTable.STREAM_TYPE_ADTS, createAdtsConsumer(rep));
		map.put(ProgramMapTable.STREAM_TYPE_H264, createH264Consumer(rep));
		UnhandledStreamTSPacketConsumer defaultStreamProc = new UnhandledStreamTSPacketConsumer();
		defaultStreamProc.setPesConsumer(new ValidatingPesConsumer(rep));
		defaultStreamProc.setReporter(rep);
		StreamProcRegistry streamProcRegistry = new StreamProcRegistry(map, defaultStreamProc);
		MultiTSPacketConsumer consumer = new MultiTSPacketConsumer(
			new TSPacketValidator(rep),
			pidFilter.filter(0, new FilterEntry(new PATConsumer(pidFilter, streamProcRegistry), new TransportContext()))
			         .filter(0x1fff, new FilterEntry(TSPacketConsumer.NULL, null))
		);
		return consumer;
	}

	private static PesTSPacketConsumer createAdtsConsumer(Reporter rep) {
		AacAdtsFrameConsumer aacAdtsFrameConsumer = new AacAdtsFrameConsumer();
		aacAdtsFrameConsumer.setReporter(rep);
		AdtsFrameConsumer adtsFrameConsumer = new AdtsFrameConsumer.Multi(new ValidatingAdtsFrameConsumer(rep), aacAdtsFrameConsumer);
		AdtsPesConsumer adtsConsumer = new AdtsPesConsumer(adtsFrameConsumer);
		return new PesTSPacketConsumer(adtsConsumer);
	}

	private static PesTSPacketConsumer createH264Consumer(Reporter rep) {
		Map<UnitType,NalUnitConsumer> nalUnitConsumers = new HashMap<>();
		//ValidatingNalUnitConsumer nalUnitConsumer = new ValidatingNalUnitConsumer(rep);
		Map<Integer, SeiHeaderConsumer> seiConsumers = new HashMap<>();
		//seiConsumers.put(SeiHeader.PIC_TIMING, new PicTimingSeiConsumer(picTiming));
		SeiNalUnitConsumer seiNalUnitConsumer = new SeiNalUnitConsumer(seiConsumers);
		nalUnitConsumers.put(UnitType.SEI, seiNalUnitConsumer);
		SeqParamSetNalUnitConsumer seqParamSetNalUnitConsumer = new SeqParamSetNalUnitConsumer();
		nalUnitConsumers.put(UnitType.SEQ_PARAMETER_SET, seqParamSetNalUnitConsumer);
		PESConsumer.MultiPesConsumer consumers
			= new PESConsumer.MultiPesConsumer(
				new ValidatingPesConsumer(rep),
				new H264PesConsumer(nalUnitConsumers)
			);
		return new PesTSPacketConsumer(consumers);
	}
}
