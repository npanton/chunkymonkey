package uk.co.badgersinfoil.chunkymonkey.rtp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.rtp.FecPacket.Direction;
import uk.co.badgersinfoil.chunkymonkey.rtp.RtpBuffer.RtpBufferContext;
import uk.co.badgersinfoil.chunkymonkey.ts.BufferTransportStreamParser;

public class RtpParser {
	public static class RtpStreamLocator implements Locator {

		private long ssrc;
		private Locator parent;

		public RtpStreamLocator(long ssrc, Locator parent) {
			this.ssrc = ssrc;
			this.parent = parent;
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			b.append("RTP Stream ssrc=").append(ssrc);
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
	public static class RtpContext implements MediaContext {
		private long ssrc = -1;
		private int lastSeq = -1;
		private long lastTimestamp = -1;
		private Set<Long> bannedSsrcs = new HashSet<>();
//		private MediaContext consumerContext;
		public CorrectionMatrix correctionMatrix;
		public long fecSsrc = -1;
		private Set<Long> bannedFecSsrcs = new HashSet<>();
		public int lastFecSeq = -1;
		private MediaContext parentContext;
		public RtpBufferContext rtpBufferContext;

		public RtpContext(BufferTransportStreamParser consumer, MediaContext parentContext) {
			this.parentContext = parentContext;
			correctionMatrix = new CorrectionMatrix(consumer);
		}

		/**
		 * Defines the required 32-bit synchronisation source identifier
		 * (packets with any other values will be filtered out).  If
		 * unspecified, the SSRC of initial of the first RTP packet
		 * seen will be used.
		 */
		public void setSsrc(long ssrc) {
			this.ssrc = ssrc;
		}

		@Override
		public Locator getLocator() {
			return new RtpStreamLocator(ssrc, parentContext.getLocator());
		}
	}

	public class FecPacketLcator implements Locator {

		private FecPacket fecPkt;
		private Locator parent;

		public FecPacketLcator(FecPacket fecPkt, Locator parent) {
			this.fecPkt = fecPkt;
			this.parent = parent;
		}

		@Override
		public Locator getParent() {
			return parent;
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			b.append("FEC packet seq=").append(fecPkt.snBaseLowBits())
			 .append(" ts_recovery=").append(fecPkt.tsRecovery());
			Locator parent = getParent();
			if (parent != null) {
				b.append("\n  at ");
				b.append(parent);
			}
			return b.toString();
		}
	}

	public static class CorrectionMatrix {

		private BufferTransportStreamParser consumer;
		private boolean dimensionInitialised = false;
		private int cols;
		private int rows;
		private RtpBuffer rtpBuffer;
		public List<FecPacket> fecQueue = new ArrayList<FecPacket>();

		public CorrectionMatrix(BufferTransportStreamParser consumer) {
			this.consumer = consumer;
			rtpBuffer = new RtpBuffer(5 * 5 * 2);
		}
		private void setDimension(int rows, int cols) {
			this.rows = rows;
			this.cols = cols;
			rtpBuffer.setMaxBufferedPackets(cols * rows * 2);
		}
		public int getCols() {
			return cols;
		}
		public int getRows() {
			return rows;
		}

		/**
		 * Called with content of a FEC packet
		 */
		public void fecPacket(RtpContext ctx, FecPacket fecPkt) {
			if (!dimensionInitialised) {
				setDimension(fecPkt.offset(),
				             fecPkt.numberAssociations());
			}
			if (!RtpUtil.seqWrapLikely(fecPkt.snBaseLowBits(), rtpBuffer.minSeqNumber()) && RtpUtil.seqLikelyEarlier(fecPkt.snBaseLowBits(), rtpBuffer.minSeqNumber())) {
				System.out.println("minSeqNumber="+fecPkt.snBaseLowBits()+" is before the start of buffer @seq="+rtpBuffer.minSeqNumber()+" seqlen="+rtpBuffer.diffSeq());
				return;
			}
			if (!fecQueue.isEmpty() || fecPreceedsMedia(fecPkt)) {
				fecQueue.add(fecPkt);
			} else {
				processFec(ctx, fecPkt);
			}
		}

		private void processFec(RtpContext ctx, FecPacket fecPkt) {
			int missing = 0;
			int missingSeq = -1;
			int missingRow = -1;
			// look for missing media packets in the column covered
			// by this FEC packet
			int seq = fecPkt.snBaseLowBits();
			for (int i = 0; i<rows; i++) {
				ByteBuf p = rtpBuffer.getSeq(seq);
				if (p == null) {
					missing++;
					missingSeq = seq;
					missingRow = i;
				}
				// skip to next row,
				seq = RtpUtil.seqAdd(seq, cols);
			}
			if (missing == 1) {
				// recover
				System.out.println("FEC 1 missing packet - recoverable error seq="+missingSeq+" row="+missingRow);
				recoverMissing(ctx,
				               fecPkt.snBaseLowBits(),
				               fecPkt.payload(),
				               missingSeq);
			} else if (missing > 1) {
				// unrecoverable
				System.out.println("FEC "+missing + " missing packets (last being seq:"+missingSeq+") - unrecoverable error");
			}
		}

		/**
		 * Returns true if the last media packet with which the given
		 * FEC packet is associated has not been seen yet.
		 */
		private boolean fecPreceedsMedia(FecPacket fecPkt) {
			return RtpUtil.seqLikelyEarlier(rtpBuffer.maxSeqNumber(),
			                                RtpUtil.seqAdd(fecPkt.snBaseLowBits(), (rows-1)*cols));
		}

		private void recoverMissing(RtpContext ctx,
		                            final int minSeqNumber,
		                            final ByteBuf payload,
		                            int missingSeq)
		{
			ByteBuf tmp = Unpooled.copiedBuffer(payload);
			int seq = minSeqNumber;
			for (int i = 0; i<rows; i++) {
				ByteBuf p = rtpBuffer.getSeq(seq);
				if (p != null) {
					xor(tmp, p);
				}
				seq = RtpUtil.seqAdd(seq, cols);
			}
			rtpBuffer.recover(ctx, consumer, missingSeq, tmp);
		}

		private void xor(ByteBuf dst, ByteBuf src) {
			for (int i = 0; i < dst.readableBytes(); i++) {
				dst.setByte(i, dst.getByte(i) ^ src.getByte(i));
			}
		}

		/**
		 * Called with a media packet
		 */
		public void packet(RtpContext ctx, RtpPacket p) {
			rtpBuffer.add(ctx.rtpBufferContext, consumer, p);
			processQueuedFec(ctx);
		}
		private void processQueuedFec(RtpContext ctx) {
			for (Iterator<FecPacket> i = fecQueue.iterator(); i.hasNext(); ) {
				FecPacket p = i.next();
				if (fecPreceedsMedia(p)) {
					break;
				}
				i.remove();
				processFec(ctx, p);
			}
		}
	}

	private BufferTransportStreamParser consumer;
	private Reporter rep = Reporter.NULL;

	public RtpParser(BufferTransportStreamParser consumer) {
		this.consumer = consumer;
	}

	public void packet(RtpContext ctx, ByteBuf buf) {
		RtpPacket p = new RtpPacket(buf);
		if (ctx.ssrc == -1) {
			ctx.ssrc = p.ssrc();
		} else if (ctx.ssrc != p.ssrc()) {
			// only report an unexpected SSRC value the first time
			// its seen,
			if (!ctx.bannedSsrcs.contains(p.ssrc())) {
				ctx.bannedSsrcs.add(p.ssrc());
//				err.unexpectedSsrc(loc, ctx.ssrc, p.ssrc());
System.err.println("unexpected SSRC "+p.ssrc()+", wanted "+ctx.ssrc+" (seq:"+p.sequenceNumber()+")");
			}
		} else {
			if (ctx.lastSeq != -1) {
				int expected = RtpUtil.seqInc(ctx.lastSeq);
				if (expected != p.sequenceNumber()) {
					int missing = RtpUtil.seqDiff(expected, p.sequenceNumber());
System.err.println(missing+" packets missing.  Last "+ctx.lastSeq+", this "+p.sequenceNumber());
				}
			}
			ctx.lastSeq = p.sequenceNumber();
			ctx.correctionMatrix.packet(ctx, p);
		}
	}

	public void fecPacket(RtpContext ctx, ByteBuf buf) {
		RtpPacket p = new RtpPacket(buf);
		if (ctx.fecSsrc == -1) {
			ctx.fecSsrc = p.ssrc();
		} else if (ctx.fecSsrc != p.ssrc()) {
			// only report an unexpected SSRC value the first time
			// its seen,
			if (!ctx.bannedFecSsrcs.contains(p.ssrc())) {
				ctx.bannedFecSsrcs.add(p.ssrc());
				rep.carp(ctx.getLocator(), "unexpected SSRC %d, expecting %d (seq:%d)", p.ssrc(), ctx.fecSsrc, p.sequenceNumber());
			}
			return;
		}

		if (ctx.lastFecSeq != -1) {
			int expected = RtpUtil.seqInc(ctx.lastFecSeq);
			if (expected != p.sequenceNumber()) {
				int missing = RtpUtil.seqDiff(expected, p.sequenceNumber());
System.err.println(missing+" FEC packets missing.  Last "+ctx.lastFecSeq+", this "+p.sequenceNumber());
			}
		}
		ctx.lastFecSeq  = p.sequenceNumber();

		ByteBuf fecbuf = p.payload();
		FecPacket header = new FecPacket(fecbuf);
		if (header.direction() == Direction.COLS) {
			ctx.correctionMatrix.fecPacket(ctx, header);
		} else {
			rep.carp(new FecPacketLcator(header, ctx.getLocator()), "Packet specifies, %s, but only %s is supported", header.direction(), Direction.COLS);
		}
	}

	public void setReporter(Reporter rep) {
		this.rep = rep;
	}

	public RtpContext createContext(MediaContext ctx) {
		RtpContext rtpContext = new RtpContext(consumer, ctx);
		RtpBufferContext rtpBufferContext = new RtpBufferContext(rtpContext);
		rtpContext.rtpBufferContext = rtpBufferContext;
		rtpBufferContext.setConsumerContext(consumer.createContext(rtpBufferContext));
		return rtpContext;
	}
}
