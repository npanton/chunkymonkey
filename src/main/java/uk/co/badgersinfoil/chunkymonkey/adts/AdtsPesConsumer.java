package uk.co.badgersinfoil.chunkymonkey.adts;

import io.netty.buffer.ByteBuf;
import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.ts.ElementryContext;
import uk.co.badgersinfoil.chunkymonkey.ts.PESConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket;

public class AdtsPesConsumer implements PESConsumer {

	public class ADTSElementryContext implements ElementryContext {
		private ADTSFrame adtsFrame;
		private PESPacket currentPesPacket;
		private int frameNumber;
		public boolean continuityError;
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

	public AdtsPesConsumer(AdtsFrameConsumer consumer) {
		this.consumer = consumer;
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
	}

	private void parsePacket(ADTSElementryContext adtsCtx, ByteBuf buf) {
		while (buf != null) {
			if (adtsCtx.adtsFrame == null) {
				Locator loc = new AdtsLocator(adtsCtx.currentPesPacket.getLocator(), adtsCtx.frameNumber++);
				adtsCtx.adtsFrame = new ADTSFrame(loc, buf);
			} else {
				adtsCtx.adtsFrame.append(buf);
			}
			if (adtsCtx.adtsFrame.isHeaderComplete() && adtsCtx.adtsFrame.syncWord() != 0xfff) {
				// TODO: inform consumer and clear out adtsCtx
				return;
			}
			if (adtsCtx.adtsFrame.isComplete()) {
				consumer.frame(adtsCtx.adtsFrame);
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
System.err.println("ADTS frames in this PES packet = "+(adtsCtx.frameNumber+1));
	}

	@Override
	public void continuityError(ElementryContext ctx) {
		ADTSElementryContext adtsCtx = (ADTSElementryContext)ctx;
		adtsCtx.continuityError = true;
	}

	@Override
	public ElementryContext createContext() {
		return new ADTSElementryContext();
	}
}
