package uk.co.badgersinfoil.chunkymonkey.snickersnack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import uk.co.badgersinfoil.chunkymonkey.ConsoleReporter;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.URILocator;
import uk.co.badgersinfoil.chunkymonkey.h264.H264PesConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.NalUnitConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.PicTimingSeiConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.SeiHeader;
import uk.co.badgersinfoil.chunkymonkey.h264.SeiHeaderConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.SeiNalUnitConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.SeqParamSetNalUnitConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.NALUnit.UnitType;
import uk.co.badgersinfoil.chunkymonkey.ts.FileTransportStreamParser;
import uk.co.badgersinfoil.chunkymonkey.ts.MultiTSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PATConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PESConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PIDFilterPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PesTSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.ProgramMapTable;
import uk.co.badgersinfoil.chunkymonkey.ts.StreamProcRegistry;
import uk.co.badgersinfoil.chunkymonkey.ts.StreamTSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacketValidator;
import uk.co.badgersinfoil.chunkymonkey.ts.TransportContext;
import uk.co.badgersinfoil.chunkymonkey.ts.UnhandledStreamTSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.ValidatingPesConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PIDFilterPacketConsumer.FilterEntry;

/**
 * One, two! One, two! and through and through <br />
 * The vorpal blade went snicker-snack!
 */
public class Main {

	public static void main(String[] args) throws IOException, URISyntaxException {
		String file = "/tmp/bbc_one_hd-NCO-a_1390564899_1390565246.ts";
		MultiTSPacketConsumer consumer = createConsumer();
		benchmark(file, consumer);
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
	private static MultiTSPacketConsumer createConsumer() {
		Reporter rep = new ConsoleReporter();
		PIDFilterPacketConsumer pidFilter = new PIDFilterPacketConsumer(rep);
		Map<Integer, StreamTSPacketConsumer> map = new HashMap<>();
		ChunkingTSPacketConsumer chunker = new ChunkingTSPacketConsumer();
		MyPCRWatchingTSConsumer pcrWatcher = new MyPCRWatchingTSConsumer(chunker);
		map.put(ProgramMapTable.STREAM_TYPE_H264, createH264Consumer(rep, chunker, pcrWatcher));
		UnhandledStreamTSPacketConsumer defaultStreamProc = new UnhandledStreamTSPacketConsumer();
		defaultStreamProc.setPesConsumer(new ValidatingPesConsumer(rep));
		defaultStreamProc.setReporter(rep);
		StreamProcRegistry streamProcRegistry = new StreamProcRegistry(map, defaultStreamProc);
		MultiTSPacketConsumer consumer = new MultiTSPacketConsumer(
			new TSPacketValidator(rep),
			pidFilter.filter(0, new FilterEntry(new PATConsumer(pidFilter, streamProcRegistry), new TransportContext()))
			         .filter(0x1fff, new FilterEntry(TSPacketConsumer.NULL, null)),
			pcrWatcher,
			chunker
		);
		return consumer;
	}

	private static PesTSPacketConsumer createH264Consumer(Reporter rep, ChunkingTSPacketConsumer chunker, MyPCRWatchingTSConsumer pcrWatcher) {
		Map<UnitType,NalUnitConsumer> nalUnitConsumers = new HashMap<>();
		//ValidatingNalUnitConsumer validatingNalUnitConsumer = new ValidatingNalUnitConsumer(rep);
		Map<Integer, SeiHeaderConsumer> seiConsumers = new HashMap<>();
		MyPicTimingConsumer picTiming = new MyPicTimingConsumer(pcrWatcher);
		seiConsumers.put(SeiHeader.PIC_TIMING, new PicTimingSeiConsumer(picTiming));
		SeiNalUnitConsumer seiNalUnitConsumer = new SeiNalUnitConsumer(seiConsumers);
		nalUnitConsumers.put(UnitType.SEI, seiNalUnitConsumer);
		SeqParamSetNalUnitConsumer seqParamSetNalUnitConsumer = new SeqParamSetNalUnitConsumer();
		nalUnitConsumers.put(UnitType.SEQ_PARAMETER_SET, seqParamSetNalUnitConsumer);
		PesSwitchConsumer h264Switch = new PesSwitchConsumer(new H264PesConsumer(nalUnitConsumers));
		pcrWatcher.setH264Switch(h264Switch);
		PESConsumer.MultiPesConsumer consumers
			= new PESConsumer.MultiPesConsumer(
				new ValidatingPesConsumer(rep),
				h264Switch
			);
		return new PesTSPacketConsumer(consumers);
	}
}
