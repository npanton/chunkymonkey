package uk.co.badgersinfoil.chunkymonkey.adts;

import io.netty.buffer.ByteBuf;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.MediaDuration;
import uk.co.badgersinfoil.chunkymonkey.MediaTimestamp;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.snickersnack.MyPicTimingConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.ElementryContext;
import uk.co.badgersinfoil.chunkymonkey.ts.PESConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket.Timestamp;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket;

public class AdtsPesConsumer implements PESConsumer {

	public class ADTSElementryContext implements ElementryContext {
		private MediaContext parentContext;
		private ADTSContext adtsContext;
		private ADTSFrame adtsFrame;
		private PESPacket currentPesPacket;
		private MediaTimestamp lastPts;
		private int frameNumber;
		public boolean continuityError;
		public MediaTimestamp currentPts;
		public MediaDuration lastPesDuration;
		public MediaDuration lastElapsedDuration;

		public ADTSElementryContext(MediaContext parentContext) {
			this.parentContext = parentContext;
		}

		@Override
		public Locator getLocator() {
			return new AdtsLocator(parentContext.getLocator(), frameNumber);
		}
	}

	public class AdtsLocator implements Locator {

		private Locator parent;
		private int frameIndex;

		public AdtsLocator(Locator parent, int frameIndex) {
			this.parent = parent;
			this.frameIndex = frameIndex;
		}

		@Override
		public Locator getParent() {
			return parent;
		}

		@Override
		public String toString() {
			return "ADTS Frame #"+frameIndex+"\n  at "+parent.toString();
		}
	}

	private AdtsFrameConsumer consumer;
	private Reporter rep = Reporter.NULL;

	public AdtsPesConsumer(AdtsFrameConsumer consumer) {
		this.consumer = consumer;
	}

	public void setReportor(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void start(ElementryContext ctx, PESPacket pesPacket) {
		ADTSElementryContext adtsCtx = (ADTSElementryContext)ctx;
		adtsCtx.currentPesPacket = pesPacket;
		adtsCtx.frameNumber = 0;
		adtsCtx.continuityError = false;
		if (adtsCtx.adtsFrame != null) {
			// TODO: use a Reporter instance?
			System.err.println("last ADTS frame from previous PES packet not complete on new PES packet start");
			adtsCtx.adtsFrame = null;
		}
		ByteBuf buf = pesPacket.getParsedPESPaload().getContent();
		parsePacket(adtsCtx, buf);

		if (adtsCtx.currentPesPacket.getParsedPESPaload().ptsDdsFlags().isPtsPresent()) {
			Timestamp pts = adtsCtx.currentPesPacket.getParsedPESPaload().pts();
			if (pts.isValid()) {
				// TODO: move PTS_UNITS somewhere sensible
				adtsCtx.currentPts = new MediaTimestamp(pts.getTs(), MyPicTimingConsumer.PTS_UNITS);
			}
		}
		checkPtsVsMediaTime(adtsCtx, pesPacket);
	}

	private void checkPtsVsMediaTime(ADTSElementryContext adtsCtx,
	                                 PESPacket pesPacket)
	{
		if (adtsCtx.currentPts != null && adtsCtx.lastPts != null) {
			MediaDuration ptsDiff = adtsCtx.currentPts.diff(adtsCtx.lastPts);
			if (adtsCtx.lastPesDuration != null) {
				MediaDuration pesDuration = ptsDiff.units().convert(adtsCtx.lastPesDuration);
				long ptsVsMedia = ptsDiff.value() - pesDuration.value();
				// assume that a difference of ~1 tick will be
				// due to some kind of rounding problem, and ignore
				if (Math.abs(ptsVsMedia) > 1) {
					rep.carp(adtsCtx.getLocator(), "PTS change (from %s to %s) unequal to duration of ADTS data in preceeding PES packet (%s); a difference of %d ticks", adtsCtx.lastPts, adtsCtx.currentPts, adtsCtx.lastPesDuration, ptsVsMedia);
				}
			}
			adtsCtx.lastPesDuration = null;
		}
	}

	private void parsePacket(ADTSElementryContext adtsCtx, ByteBuf buf) {
		while (buf != null) {
			if (adtsCtx.adtsFrame == null) {
				adtsCtx.frameNumber++;
				adtsCtx.adtsFrame = new ADTSFrame(buf);
			} else {
				adtsCtx.adtsFrame.append(buf);
			}
			if (adtsCtx.adtsFrame.isHeaderComplete() && adtsCtx.adtsFrame.syncWord() != 0xfff) {
				// TODO: inform consumer and clear out adtsCtx
				return;
			}
			if (adtsCtx.adtsFrame.isComplete()) {
				consumer.frame(adtsCtx.adtsContext, adtsCtx.adtsFrame);
				// the remaining data, if any, is part of the next frame,
				buf = adtsCtx.adtsFrame.trailingData();
				adtsCtx.adtsFrame = null;
			} else {
				buf = null;
			}
		}
	}

	@Override
	public void continuation(ElementryContext ctx, TSPacket packet, ByteBuf buf) {
		ADTSElementryContext adtsCtx = (ADTSElementryContext)ctx;
		if (!adtsCtx.continuityError) {
			parsePacket(adtsCtx, buf);
		}
	}

	@Override
	public void end(ElementryContext ctx) {
		ADTSElementryContext adtsCtx = (ADTSElementryContext)ctx;
		if (adtsCtx.lastElapsedDuration == null) {
			adtsCtx.lastPesDuration = adtsCtx.adtsContext.getDuration();
		} else {
			adtsCtx.lastPesDuration = adtsCtx.adtsContext.getDuration().minus(adtsCtx.lastElapsedDuration);
		}
		adtsCtx.lastElapsedDuration = adtsCtx.adtsContext.getDuration();
		adtsCtx.lastPts = adtsCtx.currentPts;
	}

	@Override
	public void continuityError(ElementryContext ctx) {
		ADTSElementryContext adtsCtx = (ADTSElementryContext)ctx;
		adtsCtx.continuityError = true;
		adtsCtx.lastPts = null;
	}

	@Override
	public ElementryContext createContext(MediaContext parentContext) {
		ADTSElementryContext adtsElementryContext = new ADTSElementryContext(parentContext);
		adtsElementryContext.adtsContext = consumer.createContext(adtsElementryContext);
		return adtsElementryContext;
	}
}
