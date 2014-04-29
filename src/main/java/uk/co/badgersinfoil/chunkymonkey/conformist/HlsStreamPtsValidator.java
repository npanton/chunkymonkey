package uk.co.badgersinfoil.chunkymonkey.conformist;

import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsValidatingPesConsumer.HlsValidatingPesContext;
import uk.co.badgersinfoil.chunkymonkey.snickersnack.MyPicTimingConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.ElementryContext;
import uk.co.badgersinfoil.chunkymonkey.ts.MultiTSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.MultiTSPacketConsumer.MultiTsContext;
import uk.co.badgersinfoil.chunkymonkey.ts.MultiTSPacketConsumer.MultiTsContext.Entry;
import uk.co.badgersinfoil.chunkymonkey.ts.PESConsumer.MultiPesConsumer.MultiElementryContext;
import uk.co.badgersinfoil.chunkymonkey.ts.PesTSPacketConsumer.PESLocator;
import uk.co.badgersinfoil.chunkymonkey.ts.PesTSPacketConsumer.PesStreamTSContext;
import uk.co.badgersinfoil.chunkymonkey.ts.TSContext;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacketConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.TransportContext;

public class HlsStreamPtsValidator implements TSPacketConsumer {

	private class PCRtoPTSValidatorContext implements TSContext {

		public MultiTsContext multiTSPacketContext;

		public PCRtoPTSValidatorContext(TSContext parent,
		                                TSContext multiTSPacketContext)
		{
			this.multiTSPacketContext = (MultiTsContext)multiTSPacketContext;
		}

	}

	private MultiTSPacketConsumer multiTSPacketConsumer;
	private Reporter rep;

	public HlsStreamPtsValidator(MultiTSPacketConsumer consumer, Reporter rep) {
		this.multiTSPacketConsumer = consumer;
		this.rep = rep;
	}

	@Override
	public void packet(TSContext ctx, TSPacket packet) {
		PCRtoPTSValidatorContext vctx = (PCRtoPTSValidatorContext)ctx;
		multiTSPacketConsumer.packet(vctx.multiTSPacketContext, packet);
	}

	@Override
	public void end(TSContext ctx) {
		PCRtoPTSValidatorContext vctx = (PCRtoPTSValidatorContext)ctx;
		multiTSPacketConsumer.end(vctx.multiTSPacketContext);
		checkInitialPtsValues(vctx);
	}

	private void checkInitialPtsValues(PCRtoPTSValidatorContext vctx) {
		HlsValidatingPesContext firstStream = null;
		// FIXME: this is sensitive to changes in AppBuilder
		//        (Visitor design pattern or similar for context
		//         objects required?)
		for (Entry e : vctx.multiTSPacketContext.list) {
			TSContext ctx = e.getContext();
			if (ctx instanceof TransportContext) {
				TransportContext tsCtx = (TransportContext)ctx;
				for (TSContext filterContext : tsCtx.getContexts()) {
					if (filterContext instanceof PesStreamTSContext) {
						ElementryContext elementryContext = ((PesStreamTSContext)filterContext).getElementryContext();
						if (elementryContext instanceof MultiElementryContext) {
							MultiElementryContext meCtx = (MultiElementryContext)elementryContext;
							for (ElementryContext me : meCtx.getContexts()) {
								if (me instanceof HlsValidatingPesContext) {
									HlsValidatingPesContext hlsCtx = (HlsValidatingPesContext)me;
									if (firstStream == null && hlsCtx.initialPts != null) {
										firstStream = hlsCtx;
									} else {
										if (hlsCtx.initialPts != null && hlsCtx.initialPts.getTs() != firstStream.initialPts.getTs()) {
											rep.carp(hlsCtx.initialPacket.getLocator(),
											         "HLS segment initial PTS %d for this stream and initial PTS %d for stream PID=%d differ by %dµs",
											         hlsCtx.initialPts.getTs(),
											         firstStream.initialPts.getTs(),
											         ((PESLocator)firstStream.initialPacket.getLocator()).getElementryPID(),
											         millisDiff(firstStream.initialPts.getTs(), hlsCtx.initialPts.getTs()));
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private long millisDiff(long pts1, long pts2) {
		return MyPicTimingConsumer.PTS_UNITS.toMicros(pts1-pts2);
	}

	@Override
	public TSContext createContext(TSContext parent) {
		return new PCRtoPTSValidatorContext(parent, multiTSPacketConsumer.createContext(parent));
	}
}
