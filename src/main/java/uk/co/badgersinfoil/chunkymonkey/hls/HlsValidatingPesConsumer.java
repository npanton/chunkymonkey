package uk.co.badgersinfoil.chunkymonkey.hls;

import io.netty.buffer.ByteBuf;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.ts.ElementryContext;
import uk.co.badgersinfoil.chunkymonkey.ts.PESConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket.Timestamp;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket;

public class HlsValidatingPesConsumer implements PESConsumer {

	public static class HlsValidatingPesContext implements ElementryContext {
		public int packetCount = 0;
		public Timestamp initialPts;
		public Locator initialPesLocator;
		private MediaContext parentContext;

		public HlsValidatingPesContext(MediaContext parentContext) {
			this.parentContext = parentContext;
		}

		@Override
		public Locator getLocator() {
			return initialPesLocator;
		}
	}

	private Reporter rep;

	public HlsValidatingPesConsumer(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void start(ElementryContext ctx, PESPacket pesPacket) {
		HlsValidatingPesContext vctx = (HlsValidatingPesContext)ctx;
		if (vctx.packetCount == 0 && pesPacket.isParsed()) {
			if (pesPacket.getParsedPESPaload().ptsDdsFlags().isPtsPresent()) {
				vctx.initialPts = pesPacket.getParsedPESPaload().pts();
				vctx.initialPesLocator = vctx.parentContext.getLocator();
			} else {
				rep.carp(vctx.getLocator(), "Initial PES packet in HLS segment lacks a Presentation Timestamp");
			}
		}
else if (vctx.packetCount == 0 && !pesPacket.isParsed()) {
	// added to work out why HlsStreamPtsValidator needs to check for initialPts being null
	// (it's probably absolutely fine, so not really correct to carp about it)
	rep.carp(vctx.getLocator(), "DEBUG: Could not record initialPts because this is not a parsed PES packet");
}
		vctx.packetCount++;
	}

	@Override
	public void continuation(ElementryContext ctx,
	                         TSPacket packet,
	                         ByteBuf payload)
	{
	}

	@Override
	public void end(ElementryContext ctx) {
	}

	@Override
	public ElementryContext createContext(MediaContext parentContext) {
		return new HlsValidatingPesContext(parentContext);
	}

	@Override
	public void continuityError(ElementryContext ctx) {
	}
}
