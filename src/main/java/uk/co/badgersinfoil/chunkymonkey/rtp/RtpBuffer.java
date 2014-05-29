package uk.co.badgersinfoil.chunkymonkey.rtp;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.rtp.RtpParser.RtpContext;
import uk.co.badgersinfoil.chunkymonkey.ts.BufferTransportStreamParser;

public class RtpBuffer {

	public static class FecRecoveredLocator implements Locator {

		private Locator parent;
		private int seq;

		public FecRecoveredLocator(int seq, Locator parent) {
			this.parent = parent;
			this.seq = seq;
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			b.append("FEC recovered RTP Packet seq=").append(seq);
			Locator parent = getParent();
			if (parent != null) {
				b.append("\n  at ");
				b.append(parent);
			}
			return b.toString();
		}

		@Override
		public Locator getParent() {
			return parent;
		}

	}
	public static class RtpLocator implements Locator {

		private Locator parent;
		private int seq;

		public RtpLocator(int seq, Locator parent) {
			this.seq = seq;
			this.parent = parent;
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			b.append("RTP Packet seq=").append(seq);
			Locator parent = getParent();
			if (parent != null) {
				b.append("\n  at ");
				b.append(parent);
			}
			return b.toString();
		}

		@Override
		public Locator getParent() {
			return parent;
		}

	}

	public static class RtpBufferContext implements MediaContext {

		private MediaContext parentContext;
		private MediaContext consumerContext;
		private BufferEntry lastEntry;

		public RtpBufferContext(MediaContext parentContext) {
			this.parentContext = parentContext;
		}

		@Override
		public Locator getLocator() {
			return lastEntry.recovered
				? new FecRecoveredLocator(lastEntry.seq, parentContext.getLocator())
				: new RtpLocator(lastEntry.seq, parentContext.getLocator());
		}

		public MediaContext consumerContext() {
			return consumerContext;
		}

		public void setConsumerContext(MediaContext consumerContext) {
			this.consumerContext = consumerContext;
		}
	}

	public class BufferEntry {

		public int seq;
		public boolean consumed = false;
		private ByteBuf payload;
		public boolean recovered;

		public BufferEntry(int seq) {
			this.seq = seq;
		}

		public BufferEntry(int seq, ByteBuf payload) {
			this.seq = seq;
			this.payload = payload;
		}

		public void maybeConsume(RtpBufferContext ctx, BufferTransportStreamParser consumer) {
			if (!consumed) {
				if (payload == null) {
//					consumer.discontinuity(ctx.consumerContext);
				} else {
					ctx.lastEntry = this;
					consumer.buffer(ctx.consumerContext(), payload);
				}
			}
			consumed = true;
		}

		public void consume(RtpBufferContext ctx, BufferTransportStreamParser consumer) {
			if (consumed) {
				throw new IllegalStateException("already consumed");
			}
			ctx.lastEntry = this;
			consumer.buffer(ctx.consumerContext(), payload);
			consumed = true;
		}
	}

	private ArrayList<BufferEntry> packets;
	private int maxPackets;

	public RtpBuffer(int maxBufferedPackets) {
		if (maxBufferedPackets < 1) {
			throw new IllegalArgumentException("maxBufferedPackets too small: "+maxBufferedPackets);
		}
		this.maxPackets = maxBufferedPackets;
		packets = new ArrayList<BufferEntry>(maxBufferedPackets);
	}

	public ByteBuf getSeq(int seq) {
		BufferEntry e = findSeq(seq);
		return e == null ? null : e.payload;
	}

	private BufferEntry findSeq(int seq) {
		for (BufferEntry e : packets) {
			if (e.seq == seq) {
				return e;
			}
		}
		return null;
	}

	public void setMaxBufferedPackets(int maxPackets) {
		this.maxPackets = maxPackets;
		packets.ensureCapacity(maxPackets);
	}

	public void add(RtpBufferContext ctx, BufferTransportStreamParser consumer, RtpPacket p) {
		BufferEntry e = new BufferEntry(p.sequenceNumber(), p.payload());
		e.recovered = false;
		add(ctx, consumer, e);
	}

	private void add(RtpBufferContext ctx, BufferTransportStreamParser consumer, BufferEntry add) {
		// TODO: handle reorderings and repeats
		if (!packets.isEmpty()) {
			// ideally seqDiff==1, but might be higher if we missed
			// packets,
			int seqDiff = RtpUtil.seqDiff(last().seq, add.seq);
			if (packets.size() + seqDiff > maxPackets) {
				// remove any packets at the start of the
				// buffer that are now not needed,
				int toRemove = packets.size() + seqDiff - maxPackets;
				if (toRemove > maxPackets) {
					toRemove = maxPackets;
				}
				if (toRemove > packets.size()) {
					toRemove = packets.size();
				}
				while (toRemove-- > 0) {
					packets.remove(0).maybeConsume(ctx, consumer);
				}
			}
			// consume any packets at the start of the buffer now
			// eligible for consumption because we just removed
			// a gap in the buffer above,
			for (int i=0; i < packets.size(); i++) {
				BufferEntry e = packets.get(i);
				if (e.payload == null) {
					break;
				}
				e.maybeConsume(ctx, consumer);
			}
			int missingEntries = Math.min(seqDiff, maxPackets)-1;
			int lastSequenceNumber = packets.isEmpty() ? RtpUtil.seqAdd(add.seq, -maxPackets)
			                                           : last().seq;
			// if we had a large jump in sequence numbers, we might
			// have dropped so many entries that we now need to
			// make some placeholders for the sequence numbers
			// immediately preceding the sequence number of the
			// packet we're adding,
			while (missingEntries-- > 0) {
				lastSequenceNumber = RtpUtil.seqInc(lastSequenceNumber);
				packets.add(new BufferEntry(lastSequenceNumber));
			}
		}
		// TODO: handle re-orderings and insert earlier into buffer if
		//       possible
		packets.add(add);
		if (packets.size()==1 || packets.size() > 1 && packets.get(packets.size()-2).consumed) {
			add.consume(ctx, consumer);
		}

	}

	public void recover(RtpContext ctx,
			BufferTransportStreamParser consumer, int missingSeq,
			ByteBuf recovered)
	{
		BufferEntry e = findSeq(missingSeq);
		e.payload = recovered;
		e.recovered = true;
	}

	public int minSeqNumber() {
		return packets.isEmpty() ? -1 : first().seq;
	}

	public int maxSeqNumber() {
		return packets.isEmpty() ? -1 : last().seq;
	}

	private BufferEntry last() {
		return packets.get(packets.size()-1);
	}

	private BufferEntry first() {
		return packets.get(0);
	}

	public int diffSeq() {
		if (packets.isEmpty()) {
			return -1;
		}
		return RtpUtil.seqDiff(first().seq, last().seq);
	}

	public int size() {
		return packets.size();
	}
}
