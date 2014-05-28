package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.HashMap;
import java.util.Map;

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
}
