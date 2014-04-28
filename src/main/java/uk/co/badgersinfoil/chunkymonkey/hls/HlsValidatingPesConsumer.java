package uk.co.badgersinfoil.chunkymonkey.hls;

import io.netty.buffer.ByteBuf;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.ts.ElementryContext;
import uk.co.badgersinfoil.chunkymonkey.ts.PESConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket.Timestamp;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket;

public class HlsValidatingPesConsumer implements PESConsumer {

	public static class HlsValidatingPesContext implements ElementryContext {
		public int packetCount = 0;
		public Timestamp initialPts;
		public PESPacket initialPacket;
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
				vctx.initialPacket = pesPacket;
			} else {
				rep.carp(pesPacket.getLocator(), "Initial PES packet in HLS segment lacks a Presentation Timestamp");
			}
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
	public ElementryContext createContext() {
		return new HlsValidatingPesContext();
	}

	@Override
	public void continuityError(ElementryContext ctx) {
	}
}
