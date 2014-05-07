package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.HashMap;
import java.util.Map;
import uk.co.badgersinfoil.chunkymonkey.ts.ProgramMapTable.StreamDescriptorIterator;

public class StreamProcRegistry {

	private Map<StreamType, StreamTSPacketConsumer> map = new HashMap<>();
	private StreamTSPacketConsumer defaultStreamProc;

	public StreamProcRegistry(Map<StreamType, StreamTSPacketConsumer> map, StreamTSPacketConsumer defaultStreamProc) {
		this.defaultStreamProc = defaultStreamProc;
		this.map.putAll(map);
	}

	public StreamTSPacketConsumer getStreamHandler(StreamType streamType) {
		StreamTSPacketConsumer consumer = map.get(streamType);
		if (consumer == null) {
			return defaultStreamProc;
		}
		return consumer;
	}

//	public TSPacketConsumer create(StreamDescriptorIterator i) {
//		StreamProcBuilder builder = map.get(i.streamType());
//		if (builder == null) {
//			return defaultStreamProc.create(i);
//		}
//		return builder.create(i);
//	}
}
