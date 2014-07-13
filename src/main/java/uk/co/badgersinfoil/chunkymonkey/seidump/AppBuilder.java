package uk.co.badgersinfoil.chunkymonkey.seidump;

import java.util.HashMap;
import java.util.Map;
import uk.co.badgersinfoil.chunkymonkey.event.ConsoleReporter;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.h264.H264PesConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.NalUnitConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.PicTimingSeiConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.SeiHeader;
import uk.co.badgersinfoil.chunkymonkey.h264.SeiHeaderConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.SeiNalUnitConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.SeqParamSetNalUnitConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.NALUnit.UnitType;
import uk.co.badgersinfoil.chunkymonkey.h264.ValidatingPicTimingConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.FileTransportStreamParser;
import uk.co.badgersinfoil.chunkymonkey.ts.MultiTSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PATConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PESConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PIDFilterPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PesTSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PmtConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PmtConsumerImpl;
import uk.co.badgersinfoil.chunkymonkey.ts.PmtTSPacketConsumerConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.StreamProcRegistry;
import uk.co.badgersinfoil.chunkymonkey.ts.StreamTSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.StreamType;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacketValidator;
import uk.co.badgersinfoil.chunkymonkey.ts.UnhandledStreamTSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.ValidatingPesConsumer;

public class AppBuilder {
	public MultiTSPacketConsumer createConsumer() {
		Reporter rep = new ConsoleReporter();
		PIDFilterPacketConsumer pidFilter = new PIDFilterPacketConsumer(rep);
		Map<StreamType, StreamTSPacketConsumer> map = new HashMap<>();
		map.put(StreamType.H264, createH264Consumer(rep));
		UnhandledStreamTSPacketConsumer defaultStreamProc = new UnhandledStreamTSPacketConsumer();
		defaultStreamProc.setPesConsumer(new ValidatingPesConsumer(rep));
		defaultStreamProc.setReporter(rep);
		StreamProcRegistry streamProcRegistry = new StreamProcRegistry(map, defaultStreamProc);
		PmtConsumer pmtConsumer = new PmtConsumerImpl(pidFilter, streamProcRegistry);
		PmtTSPacketConsumerConsumer pmtTsConsumer = new PmtTSPacketConsumerConsumer(pmtConsumer);
		pidFilter.defaultFilter(0, new PATConsumer(pidFilter, pmtTsConsumer))
		         .defaultFilter(0x1fff, TSPacketConsumer.NULL);
		MultiTSPacketConsumer consumer = new MultiTSPacketConsumer(
			new TSPacketValidator(rep),
			pidFilter
		);
		return consumer;
	}

	private PesTSPacketConsumer createH264Consumer(Reporter rep) {
		Map<UnitType,NalUnitConsumer> nalUnitConsumers = new HashMap<>();
		//ValidatingNalUnitConsumer validatingNalUnitConsumer = new ValidatingNalUnitConsumer(rep);
		Map<Integer, SeiHeaderConsumer> seiConsumers = new HashMap<>();
		DumpingPicTimingConsumer picTiming = new DumpingPicTimingConsumer();
		ValidatingPicTimingConsumer validatingPicTiming = new ValidatingPicTimingConsumer(rep, picTiming);
		seiConsumers.put(SeiHeader.PIC_TIMING, new PicTimingSeiConsumer(validatingPicTiming));
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

	public FileTransportStreamParser createFileParser() {
		return new FileTransportStreamParser(createConsumer());
	}

}
