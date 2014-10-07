package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import uk.co.badgersinfoil.chunkymonkey.event.Alert;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsMediaPlaylistContext;
import uk.co.badgersinfoil.chunkymonkey.hls.HlsSegmentProcessor.HlsSegmentTsContext;
import uk.co.badgersinfoil.chunkymonkey.rfc6381.AudioObjectTypeCodec;
import uk.co.badgersinfoil.chunkymonkey.rfc6381.Avc1Codec;
import uk.co.badgersinfoil.chunkymonkey.rfc6381.Rfc6381Codec;
import uk.co.badgersinfoil.chunkymonkey.ts.PmtConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.ProgramMapTable;
import uk.co.badgersinfoil.chunkymonkey.ts.ProgramMapTable.StreamDescriptorIterator;
import uk.co.badgersinfoil.chunkymonkey.ts.ProgramTSContext;
import uk.co.badgersinfoil.chunkymonkey.ts.StreamType;
import uk.co.badgersinfoil.chunkymonkey.ts.TransportStreamParser.TransportStreamParserContext;

public class HlsCodecValidatingPmtConsumer implements PmtConsumer {

	private Reporter rep;

	public HlsCodecValidatingPmtConsumer(Reporter rep) {
		this.rep = rep;
	}

	@LogFormat("No match for TS segment PMT entries {missing} in HLS manifest CODECS {codecList}")
	public static final class NoMatchForTSSegmentPMTEntriesEvent extends Alert {
	}

	@LogFormat("HLS manifest CODECS entries not found in TS segment PMT: {codecList}")
	public static final class HLSManifestCodecEntriesNoFoundInTSSegmentEvent extends Alert {
	}

	@Override
	public void handle(ProgramTSContext progCtx, ProgramMapTable pmt) {
		HlsMediaPlaylistContext hlsCtx = findHlsContext(progCtx);
		if (hlsCtx.getCodecList() == null) {
			return;
		}
		List<Rfc6381Codec> codecList = new ArrayList<Rfc6381Codec>(hlsCtx.getCodecList());
		StreamDescriptorIterator i = pmt.streamDescriptors();
		List<StreamType> missing = new ArrayList<StreamType>();
		while (i.hasNext()) {
			StreamType st = i.streamType();
			Rfc6381Codec codec = removeFirstMatch(codecList, i);
			if (codec == null) {
				missing.add(st);
			}
			i.next();
		}
		if (!missing.isEmpty()) {
			// TODO: be more forgiving about entries that the HLS
			// spec probably doesn't require to be listed in
			// the master manifest CODECS list
			new NoMatchForTSSegmentPMTEntriesEvent().at(progCtx).with("missing", missing).with("codecList", hlsCtx.getCodecList()).to(rep);
			// rep.carp(progCtx.getLocator(),
			// "No match for TS segment PMT entries %s in HLS manifest CODECS %s",
			// missing, hlsCtx.getCodecList());
		}
		if (!codecList.isEmpty()) {
			new HLSManifestCodecEntriesNoFoundInTSSegmentEvent().at(progCtx).with("codecList", codecList).to(rep);
			// rep.carp(progCtx.getLocator(),
			// "HLS manifest CODECS entries not found in TS segment PMT: %s",
			// codecList);
		}
	}

	private Rfc6381Codec removeFirstMatch(List<Rfc6381Codec> codecList, StreamDescriptorIterator desc) {
		StreamType type = desc.streamType();
		Class<? extends Rfc6381Codec> kind;
		if (type == StreamType.H264) {
			kind = Avc1Codec.class;
		} else if (type == StreamType.ADTS) {
			kind = AudioObjectTypeCodec.class;
		} else {
			return null;
		}
		for (Iterator<Rfc6381Codec> i = codecList.listIterator(); i.hasNext();) {
			Rfc6381Codec codec = i.next();
			if (kind.isInstance(codec)) {
				i.remove();
				return codec;
			}
		}
		return null;
	}

	private HlsMediaPlaylistContext findHlsContext(ProgramTSContext progCtx) {
		return ((HlsSegmentTsContext) ((TransportStreamParserContext) progCtx.getTransportContext().getParent()).getParent()).ctx;
	}
}
