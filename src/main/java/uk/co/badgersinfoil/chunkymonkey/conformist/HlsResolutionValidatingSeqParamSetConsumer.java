package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.awt.Dimension;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Event;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;
import uk.co.badgersinfoil.chunkymonkey.h264.SeqParamSet;
import uk.co.badgersinfoil.chunkymonkey.h264.SeqParamSet.FrameCrop;
import uk.co.badgersinfoil.chunkymonkey.h264.SeqParamSetConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.SeqParamSetNalUnitConsumer.SeqParamSetNalUnitContext;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMediaPlaylistContext;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsSegmentProcessor.HlsSegmentTsContext;
import uk.co.badgersinfoil.chunkymonkey.ts.PESConsumer.MultiPesConsumer.MultiElementryContext;
import uk.co.badgersinfoil.chunkymonkey.ts.PesTSPacketConsumer.PesStreamTSContext;
import uk.co.badgersinfoil.chunkymonkey.ts.TransportStreamParser.TransportStreamParserContext;
import uk.co.badgersinfoil.chunkymonkey.ts.ProgramTSContext;
import uk.co.badgersinfoil.chunkymonkey.ts.TransportContext;

/**
 * When a seq_param_set arrives in the H264 bitstream, check that the width and
 * height it specifies matches with the width and height specified in the HLS
 * Master Manifest <code>RESOLUTION</code> attribute.
 */
public class HlsResolutionValidatingSeqParamSetConsumer implements
		SeqParamSetConsumer {

	@LogFormat("Master m3u8 manifest says RESOLUTION={m3u8Width}x{m3u8Height}, but H264 seq_parameter_set says {h264Width}x{h264Height}")
	public static class ResolutionMissmatchEvent extends Event { }

	private static class SeqParamSetContext implements MediaContext {
		private SeqParamSetNalUnitContext parent;

		public SeqParamSetContext(SeqParamSetNalUnitContext parent) {
			this.parent = parent;
		}

		@Override
		public Locator getLocator() {
			return parent.getLocator();
		}
	}

	private Reporter rep;

	public HlsResolutionValidatingSeqParamSetConsumer(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public MediaContext createContext(SeqParamSetNalUnitContext parent) {
		return new SeqParamSetContext(parent);
	}

	@Override
	public void seqParamSet(MediaContext ctx, SeqParamSet params) {
		SeqParamSetContext sctx = (SeqParamSetContext)ctx;
		HlsMediaPlaylistContext playlistContext = findHlsContext(sctx);
		Dimension playlistResolution = playlistContext.getResolution();
		Dimension h264Resolution = h264Resolution(params);
		if (!playlistResolution.equals(h264Resolution)) {
			new ResolutionMissmatchEvent()
				.with("m3u8Width", playlistResolution.width)
				.with("m3u8Height", playlistResolution.height)
				.with("h264Width", h264Resolution.width)
				.with("h264Height", h264Resolution.height)
				.at(sctx)
				.to(rep);
		}
	}

	private static Dimension h264Resolution(SeqParamSet params) {
		int width  = (params.picWidthInMbsMinus1()       + 1) * 16;
		int height = (params.picHeightInMapUnitsMinus1() + 1) * 16;
		FrameCrop crop = params.frameCrop();
		if (crop != null) {
			width  -= (crop.leftOffset() * 2 + crop.rightOffset()  * 2);
			height -= (crop.topOffset()  * 2 + crop.bottomOffset() * 2);
		}
		return new Dimension(width, height);
	}


	private HlsMediaPlaylistContext findHlsContext(SeqParamSetContext sctx) {
		// TODO: This is horrible.  Probably MediaContext should just provide a getParent()
		MediaContext c = sctx.parent.getH264Context().getParentContext();
		c = ((MultiElementryContext)c).getParentContext();
		c = ((PesStreamTSContext)c).getParentContext();
		TransportContext transportContext = ((ProgramTSContext)c).getTransportContext();
		c = transportContext.getParent();
		c = ((TransportStreamParserContext)c).getParent();
		return ((HlsSegmentTsContext)c).ctx;
	}
}
