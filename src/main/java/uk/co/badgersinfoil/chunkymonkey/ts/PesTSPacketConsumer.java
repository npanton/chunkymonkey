package uk.co.badgersinfoil.chunkymonkey.ts;

import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.ts.ProgramMapTable.StreamDescriptorIterator;

public class PesTSPacketConsumer implements StreamTSPacketConsumer {

	public static class PesStreamTSContext extends StreamTSContext {
		private ProgramTSContext parentContext;
		private int elementryPID;
		private boolean payloadStarted = false;
		private StreamType streamType;
		private Reporter rep;
		private int pesPacketNo;
		private ElementryContext consumerContext;
		private int lastContinuityCount = -1;

		public PesStreamTSContext(ProgramTSContext parentContext, int elementryPID, StreamType streamType) {
			this.parentContext = parentContext;
			this.elementryPID = elementryPID;
			this.streamType = streamType;
		}

		public boolean isContinuous(TSPacket packet) {
			// continuityCounter increases for each packet with a
			// given PID that includes a payload
			return lastContinuityCount == -1
				|| (packet.adaptionControl().contentPresent()
					? packet.continuityCounter() == nextContinuityCount()
					: packet.continuityCounter() == lastContinuityCount);
		}

		private int nextContinuityCount() {
			return (lastContinuityCount + 1) & 0xf;
		}
		public void setLastContinuityCount(int lastContinuityCount) {
			this.lastContinuityCount = lastContinuityCount;
		}
		public int getLastContinuityCount() {
			return lastContinuityCount;
		}
		public ElementryContext getElementryContext() {
			return consumerContext;
		}

		@Override
		public Locator getLocator() {
			return new PESLocator(parentContext.getLocator(), elementryPID, streamType, pesPacketNo);
		}

		public ProgramTSContext getParentContext() {
			return parentContext;
		}
	}

	public static class PESLocator implements Locator {

		private Locator parent;
		private int elementryPID;
		private int pesPacketNumber;
		private StreamType streamType;

		public PESLocator(Locator parent, int elementryPID, StreamType streamType, int pesPacketNumber) {
			this.parent = parent;
			this.elementryPID = elementryPID;
			this.streamType = streamType;
			this.pesPacketNumber = pesPacketNumber;
		}

		@Override
		public Locator getParent() {
			return parent;
		}

		public int getElementryPID() {
			return elementryPID;
		}

		public StreamType getStreamType() {
			return streamType;
		}

		@Override
		public String toString() {
			return "PES Packet #"+pesPacketNumber+" (PID="+elementryPID+", "+streamType.getName()+")\n  at "+parent.toString();
		}
	}

	private PESConsumer pesConsumer;

	public PesTSPacketConsumer(PESConsumer pesConsumer) {
		this.pesConsumer = pesConsumer;
	}

	@Override
	public void packet(MediaContext tsCtx, TSPacket packet) {
		PesStreamTSContext ctx = (PesStreamTSContext)tsCtx;
		// sanity check,
		if (packet.PID() != ctx.elementryPID) {
			throw new Error("Bug: PID missmatch "+packet.PID() + "!=" + ctx.elementryPID);
		}
		if (!ctx.isContinuous(packet)) {
// TODO: push error logging responsibility elsewhere,
System.err.println(String.format("TS continuity error (PID %d) counter now %d, last value %d", packet.PID(), packet.continuityCounter(), ctx.getLastContinuityCount())+"\n  at "+tsCtx.getLocator());
			pesConsumer.continuityError(ctx.consumerContext);
		}
		ctx.setLastContinuityCount(packet.continuityCounter());
		boolean startIndicator = packet.payloadUnitStartIndicator();
		if (startIndicator) {
			if (ctx.payloadStarted) {
				pesConsumer.end(ctx.consumerContext);
			} else {
				ctx.payloadStarted = true;
			}
			PESPacket pesPacket = new PESPacket(packet.getPayload());
			ctx.pesPacketNo++;
			pesConsumer.start(ctx.consumerContext, pesPacket);
		} else if (ctx.payloadStarted && packet.adaptionControl().contentPresent() && packet.getPayloadLength() > 0) {
			// (in theory, contentPresent==true && payloadLength==0
			// is not allowed, but non-conformant streams may
			// present this combination, so we have deliberately
			// excluded that case in the condition above)
			pesConsumer.continuation(ctx.consumerContext, packet, packet.getPayload());
		}  // else, drop data for which we lack a PES header
	}

	@Override
	public void end(MediaContext tsCtx) {
		PesStreamTSContext ctx = (PesStreamTSContext)tsCtx;
		pesConsumer.end(ctx.consumerContext);
	}

	@Override
	public StreamTSContext createContext(ProgramTSContext ctx,
	                                     StreamDescriptorIterator streamDesc)
	{
		PesStreamTSContext pesStreamTSContext = new PesStreamTSContext(ctx, streamDesc.elementryPID(), streamDesc.streamType());
		pesStreamTSContext.consumerContext = pesConsumer.createContext(pesStreamTSContext);
		return pesStreamTSContext;
	}

	@Override
	public MediaContext createContext(MediaContext parent) {
		throw new RuntimeException("Oops!  Use createContext(ProgramTSContext,StreamDescriptorIterator) instead");
	}
}
