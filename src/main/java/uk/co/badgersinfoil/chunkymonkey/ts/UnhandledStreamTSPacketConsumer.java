package uk.co.badgersinfoil.chunkymonkey.ts;

import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.ts.ProgramMapTable.StreamDescriptorIterator;

public class UnhandledStreamTSPacketConsumer implements StreamTSPacketConsumer {

	public class UnhandledContext implements MediaContext {

		private MediaContext parentContext;

		public UnhandledContext(MediaContext parentContext) {
			this.parentContext = parentContext;
		}

		@Override
		public Locator getLocator() {
			return parentContext.getLocator();
		}

	}

	public static class UnhandledStreamTSPacketConsumerContext extends StreamTSContext {

		private boolean flagged;
		private StreamType streamType;
		private ProgramTSContext parentContext;

		public UnhandledStreamTSPacketConsumerContext(ProgramTSContext parentContext, StreamType streamType) {
			this.parentContext = parentContext;
			this.streamType = streamType;
		}

		@Override
		public Locator getLocator() {
			return parentContext.getLocator();
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

	@Override
	public void packet(MediaContext ctx, TSPacket packet) {
		UnhandledStreamTSPacketConsumerContext uCtx = (UnhandledStreamTSPacketConsumerContext)ctx;
		if (!uCtx.flagged) {
			rep.carp(uCtx.getLocator(), "Unhndled stream type %s", uCtx.streamType);
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
		return new UnhandledStreamTSPacketConsumerContext(ctx, streamDesc.streamType());
	}

	@Override
	public MediaContext createContext(MediaContext parent) {
		return new UnhandledContext(parent);
	}
}
