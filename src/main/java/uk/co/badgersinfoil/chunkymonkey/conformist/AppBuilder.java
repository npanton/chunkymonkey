package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.util.HashMap;
import java.util.Map;
import uk.co.badgersinfoil.chunkymonkey.ConsoleReporter;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
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

public class AppBuilder {

	public MultiTSPacketConsumer createConsumer() {
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

	private PesTSPacketConsumer createAdtsConsumer(Reporter rep) {
		AacAdtsFrameConsumer aacAdtsFrameConsumer = new AacAdtsFrameConsumer();
		aacAdtsFrameConsumer.setReporter(rep);
		AdtsFrameConsumer adtsFrameConsumer = new AdtsFrameConsumer.Multi(new ValidatingAdtsFrameConsumer(rep), aacAdtsFrameConsumer);
		AdtsPesConsumer adtsConsumer = new AdtsPesConsumer(adtsFrameConsumer);
		adtsConsumer.setReportor(rep);
		return new PesTSPacketConsumer(adtsConsumer);
	}

	private PesTSPacketConsumer createH264Consumer(Reporter rep) {
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
