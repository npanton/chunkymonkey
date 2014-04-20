package uk.co.badgersinfoil.chunkymonkey.ts;

import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.ts.ProgramMapTable.StreamDescriptorIterator;

public class PesTSPacketConsumer implements StreamTSPacketConsumer {

	public static class PesStreamTSContext extends StreamTSContext {
		private ProgramTSContext progCtx;
		private int elementryPID;
		private int streamType;
		private boolean payloadStarted = false;
		private Reporter rep;
		private PESConsumer pesConsumer;
		private int pesPacketNo;
		private ElementryContext eCtx;
		private int lastContinuityCount = -1;

		public PesStreamTSContext(ProgramTSContext progCtx, int elementryPID, ElementryContext eCtx) {
			this.progCtx = progCtx;
			this.elementryPID = elementryPID;
			this.eCtx = eCtx;
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
	}

	public static class PESLocator implements Locator {

		private Locator parent;
		private int elementryPID;
		private int pesPacketNumber;

		public PESLocator(Locator parent, int elementryPID, int pesPacketNumber) {
			this.parent = parent;
			this.elementryPID = elementryPID;
			this.pesPacketNumber = pesPacketNumber;
		}

		@Override
		public Locator getParent() {
			return parent;
		}

		@Override
		public String toString() {
			return "PES Packet #"+pesPacketNumber+" (PID="+elementryPID+")\n  at "+parent.toString();
		}
	}
	
	private PESConsumer pesConsumer;

//	private StreamContext ctx;

//	public PesTSPacketConsumer(int elementryPID, int streamType, PESConsumer pesConsumer, StreamContext ctx) {
//		this.elementryPID = elementryPID;
//		this.streamType = streamType;
//		this.pesConsumer = pesConsumer;
//		this.ctx = ctx;
//	}
	
	public PesTSPacketConsumer(PESConsumer pesConsumer) {
		this.pesConsumer = pesConsumer;
	}

	@Override
	public void packet(TSContext tsCtx, TSPacket packet) {
		PesStreamTSContext ctx = (PesStreamTSContext)tsCtx;
		// sanity check,
		if (packet.PID() != ctx.elementryPID) {
			throw new Error("Bug: PID missmatch "+packet.PID() + "!=" + ctx.elementryPID);
		}
		if (!ctx.isContinuous(packet)) {
// TODO: push error logging responsibility elsewhere,
System.err.println(String.format("TS continuity error (PID %d) counter now %d, last value %d", packet.PID(), packet.continuityCounter(), ctx.getLastContinuityCount())+"\n  at "+packet.getLocator());
			pesConsumer.continuityError(ctx.eCtx);
		}
		ctx.setLastContinuityCount(packet.continuityCounter());
		boolean startIndicator = packet.payloadUnitStartIndicator();
		if (startIndicator) {
			if (ctx.payloadStarted) {
				pesConsumer.end(ctx.eCtx);
			} else {
				ctx.payloadStarted = true;
			}
			Locator loc = new PESLocator(packet.getLocator(), ctx.elementryPID, ctx.pesPacketNo++);
			PESPacket pesPacket = new PESPacket(loc, packet.getPayload());
			pesConsumer.start(ctx.eCtx, pesPacket);
		} else if (ctx.payloadStarted && packet.adaptionControl().contentPresent() && packet.getPayloadLength() > 0) {
			// (in theory, contentPresent==true && payloadLength==0
			// is not allowed, but non-conformant streams may
			// present this combination)
			pesConsumer.continuation(ctx.eCtx, packet, packet.getPayload());
		}  // else, drop data for which we lack a PES header
	}

	@Override
	public void end(TSContext tsCtx) {
		PesStreamTSContext ctx = (PesStreamTSContext)tsCtx;
		pesConsumer.end(ctx.eCtx);
	}

	@Override
	public StreamTSContext createContext(ProgramTSContext ctx,
	                                     StreamDescriptorIterator streamDesc)
	{
		ElementryContext eCtx = pesConsumer.createContext();
		return new PesStreamTSContext(ctx, streamDesc.elementryPID(), eCtx);
	}

	@Override
	public TSContext createContext(TSContext parent) {
		throw new RuntimeException("Oops!  Use createContext(ProgramTSContext,StreamDescriptorIterator) instead");
	}
}
