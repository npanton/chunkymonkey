package uk.co.badgersinfoil.chunkymonkey.ts;

import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.ts.ProgramMapTable.StreamDescriptorIterator;

public class UnhandledStreamTSPacketConsumer implements StreamTSPacketConsumer {

	public class UnhandledContext implements MediaContext {

	}

	public static class UnhandledStreamTSPacketConsumerContext extends StreamTSContext {

		private boolean flagged;
		private StreamType streamType;

		public UnhandledStreamTSPacketConsumerContext(StreamType streamType) {
			this.streamType = streamType;
		}
	}

	private PESConsumer pesConsumer = PESConsumer.NULL;
	private Reporter rep = Reporter.NULL;

	public UnhandledStreamTSPacketConsumer() {
	}

	public void setReporter(Reporter rep) {
		this.rep = rep;
	}

	public void setPesConsumer(PESConsumer pesConsumer) {
		this.pesConsumer = pesConsumer;
	}

//	@Override
//	public TSPacketConsumer create(StreamDescriptorIterator i) {
//		rep.carp(i.getLocator(), "Unhandled stream-type %d for PID %d", i.streamType(), i.elementryPID());
//		StreamContext ctx = new StreamContext();
//		return new PesTSPacketConsumer(i.elementryPID(), i.streamType(), pesConsumer, ctx );
//	}

	@Override
	public void packet(MediaContext ctx, TSPacket packet) {
		UnhandledStreamTSPacketConsumerContext uCtx = (UnhandledStreamTSPacketConsumerContext)ctx;
		if (!uCtx.flagged) {
			rep.carp(packet.getLocator(), "Unhndled stream type %s", uCtx.streamType);
			uCtx.flagged = true;
		}
	}

	@Override
	public void end(MediaContext context) {
		// TODO Auto-generated method stub

	}

	@Override
	public StreamTSContext createContext(ProgramTSContext ctx,
			StreamDescriptorIterator streamDesc) {
		return new UnhandledStreamTSPacketConsumerContext(streamDesc.streamType());
	}

	@Override
	public MediaContext createContext(MediaContext parent) {
		return new UnhandledContext();
	}
}
