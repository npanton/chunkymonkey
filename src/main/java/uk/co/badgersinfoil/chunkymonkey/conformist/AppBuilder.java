package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.aac.AacAdtsFrameConsumer;
import uk.co.badgersinfoil.chunkymonkey.adts.AdtsFrameConsumer;
import uk.co.badgersinfoil.chunkymonkey.adts.AdtsPesConsumer;
import uk.co.badgersinfoil.chunkymonkey.adts.ValidatingAdtsFrameConsumer;
import uk.co.badgersinfoil.chunkymonkey.conformist.redundancy.HlsRedundantStreamProcessor;
import uk.co.badgersinfoil.chunkymonkey.h264.H264PesConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.NALUnit.UnitType;
import uk.co.badgersinfoil.chunkymonkey.h264.NalUnitConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.SeiHeaderConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.SeiNalUnitConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.SeqParamSetNalUnitConsumer;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMasterPlaylistProcessor;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMediaPlaylistProcessor;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsSegmentProcessor;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsTsPacketValidator;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsValidatingPesConsumer;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpExecutionWrapper;
import uk.co.badgersinfoil.chunkymonkey.hls.HttpResponseChecker;
import uk.co.badgersinfoil.chunkymonkey.rfc6381.AudioObjectTypeParser;
import uk.co.badgersinfoil.chunkymonkey.rfc6381.Avc1CodecParser;
import uk.co.badgersinfoil.chunkymonkey.rfc6381.CodecsParser;
import uk.co.badgersinfoil.chunkymonkey.rfc6381.Mp4aCodecParser;
import uk.co.badgersinfoil.chunkymonkey.rfc6381.OtiParser;
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
import uk.co.badgersinfoil.chunkymonkey.ts.UnhandledStreamTSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.ValidatingPesConsumer;

public class AppBuilder {

	public HlsMasterPlaylistProcessor buildSingle(ScheduledExecutorService scheduledExecutor, Reporter rep) {
		CloseableHttpClient httpclient
			= HttpClientBuilder.create()
			                   .setUserAgent("conformist")
			                   .setRequestExecutor(HttpExecutionWrapper.CONN_INFO_SNARFING_REQUEST_EXECUTOR)
			                   // remember 'Content-Length' from before any decompression,
			                   .addInterceptorFirst(new ContentLengthSnarfer())
			                   // add content compression support,
			                   .addInterceptorFirst(new RequestAcceptEncoding())
			                   .addInterceptorFirst(new ResponseContentEncoding())
			                   .build();
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(1000)
				.setSocketTimeout(1000)
				.build();
		HlsSegmentProcessor segProc = new HlsSegmentProcessor(rep, httpclient, createConsumer(rep));
		segProc.setManifestResponseChecker(new HttpResponseChecker.Multi(
// llnw mess with Date hdr				new CachingHeaderCheck(rep, 1),
				new CorsHeaderChecker(rep),
				new HttpMinVersionCheck(new ProtocolVersion("HTTP", 1, 1), rep),
				new ContentLengthCheck(rep),
				new CacheValidatorCheck(rep),
				new KeepAliveHeaderCheck(rep),
				new ContentTypeHeaderCheck("video/MP2T", rep)
			));
		segProc.setConfig(requestConfig);
		HlsMediaPlaylistProcessor mediaProc = new HlsMediaPlaylistProcessor(scheduledExecutor, httpclient, segProc);
		mediaProc.setManifestResponseChecker(new HttpResponseChecker.Multi(
// llnw mess with Date hdr			new CachingHeaderCheck(rep, 1),
			new CorsHeaderChecker(rep),
			new HttpMinVersionCheck(new ProtocolVersion("HTTP", 1, 1), rep),
			new ContentLengthCheck(rep),
			new CacheValidatorCheck(rep),
			new KeepAliveHeaderCheck(rep),
			new ContentTypeHeaderCheck("application/vnd.apple.mpegurl", rep)
		));
		mediaProc.setReporter(rep);
		mediaProc.setConfig(RequestConfig.custom().setConnectTimeout(1000).build());
		HlsMasterPlaylistProcessor masterProc = new HlsMasterPlaylistProcessor(scheduledExecutor, httpclient, mediaProc, createCodecsParser());
		masterProc.setResponseChecker(new HttpResponseChecker.Multi(
			new MasterPlaylistResponseChecker(rep),
			new CorsHeaderChecker(rep),
			new HttpMinVersionCheck(new ProtocolVersion("HTTP", 1, 1), rep),
			new CachingHeaderCheck(rep, 1),
			new ContentLengthCheck(rep),
			new CacheValidatorCheck(rep),
			new KeepAliveHeaderCheck(rep),
			new ContentTypeHeaderCheck("application/vnd.apple.mpegurl", rep)
		));
		masterProc.setReporter(rep);
		masterProc.setConfig(RequestConfig.custom().setConnectTimeout(1000).build());
		return masterProc;
	}

	private CodecsParser createCodecsParser() {
		OtiParser aotParser = new AudioObjectTypeParser();
		Mp4aCodecParser mp4aCodecParser = new Mp4aCodecParser();
		mp4aCodecParser.addParser("40", aotParser);
		CodecsParser parser = new CodecsParser();
		parser.addParser("mp4a", mp4aCodecParser);
		Avc1CodecParser avc1CodecParser = new Avc1CodecParser();
		parser.addParser("avc1", avc1CodecParser);
		return parser;
	}

	public HlsRedundantStreamProcessor buildRedundant(
			ScheduledExecutorService scheduledExecutor, Reporter rep) {
		HlsMasterPlaylistProcessor masterPlaylistProcessor
			= buildSingle(scheduledExecutor, rep);
		HlsRedundantStreamProcessor redundantProc
			= new HlsRedundantStreamProcessor(scheduledExecutor,
			                                  masterPlaylistProcessor,
			                                  rep);
		return redundantProc;
	}

	public TSPacketConsumer createConsumer(Reporter rep) {
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
			new HlsTsPacketValidator(rep),
			pidFilter.defaultFilter(0, new PATConsumer(pidFilter, streamProcRegistry))
			         .defaultFilter(0x1fff, TSPacketConsumer.NULL)
		);
		return new HlsStreamPtsValidator(consumer, rep);
	}

	private PesTSPacketConsumer createAdtsConsumer(Reporter rep) {
		AacAdtsFrameConsumer aacAdtsFrameConsumer = new AacAdtsFrameConsumer();
		aacAdtsFrameConsumer.setReporter(rep);
		AdtsFrameConsumer adtsFrameConsumer = new AdtsFrameConsumer.Multi(new ValidatingAdtsFrameConsumer(rep), aacAdtsFrameConsumer);
		AdtsPesConsumer adtsConsumer = new AdtsPesConsumer(adtsFrameConsumer);
		adtsConsumer.setReportor(rep);
		PESConsumer.MultiPesConsumer consumers
			= new PESConsumer.MultiPesConsumer(
				new HlsValidatingPesConsumer(rep),
				new ValidatingPesConsumer(rep),
				adtsConsumer
			);
		return new PesTSPacketConsumer(consumers);
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
				new HlsValidatingPesConsumer(rep),
				new ValidatingPesConsumer(rep),
				new H264PesConsumer(nalUnitConsumers)
			);
		return new PesTSPacketConsumer(consumers);
	}
}
