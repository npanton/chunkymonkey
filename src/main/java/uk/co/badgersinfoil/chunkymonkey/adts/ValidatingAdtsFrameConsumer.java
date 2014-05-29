package uk.co.badgersinfoil.chunkymonkey.adts;

import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.MediaDuration;
import uk.co.badgersinfoil.chunkymonkey.Reporter;

public class ValidatingAdtsFrameConsumer implements AdtsFrameConsumer {

	public class ValidatingADTSContext implements ADTSContext {
		private ADTSFrame lastFrame = null;
		private MediaContext parentContext;

		public ValidatingADTSContext(MediaContext parentContext) {
			this.parentContext = parentContext;
		}

		@Override
		public MediaDuration getDuration() {
			return null;
		}

		@Override
		public Locator getLocator() {
			return parentContext.getLocator();
		}
	}

	private Reporter rep;

	public ValidatingAdtsFrameConsumer(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void frame(ADTSContext adtsCtx, ADTSFrame frame) {
		ValidatingADTSContext ctx = (ValidatingADTSContext)adtsCtx;
		if (frame.syncWord() != 0xfff) {
			rep.carp(ctx.getLocator(), "Bad sync word 0x%s (expected 0xfff)", Integer.toHexString(frame.syncWord()));
		}
		if (ctx.lastFrame != null) {
			// the spec requires the following headers to be the
			// same in each frame of the ADTS stream (other ADTS
			// headers are allowed to vary),
			StringBuilder b = new StringBuilder();
			if (ctx.lastFrame.mpegVersion() != frame.mpegVersion()) {
				b.append(" MPEG versions differ: last=")
				 .append(ctx.lastFrame.mpegVersion())
				 .append(" this=")
				 .append(frame.mpegVersion());
			}
			if (ctx.lastFrame.layer() != frame.layer()) {
				b.append(" MPEG layer identifiers differ: last=")
				 .append(ctx.lastFrame.layer())
				 .append(" this=")
				 .append(frame.layer());
			}
			if (ctx.lastFrame.crcPresent() != frame.crcPresent()) {
				b.append(" CRC protection precence differs: last=")
				 .append(ctx.lastFrame.crcPresent())
				 .append(" this=")
				 .append(frame.crcPresent());
			}
			if (ctx.lastFrame.channelConfig() != frame.channelConfig()) {
				b.append(" channel configurations differ: last=")
				 .append(ctx.lastFrame.channelConfig())
				 .append(" this=")
				 .append(frame.channelConfig());
			}
			if (ctx.lastFrame.profile() != frame.profile()) {
				b.append(" profile differ: last=")
				 .append(ctx.lastFrame.profile())
				 .append(" this=")
				 .append(frame.profile());
			}
			if (ctx.lastFrame.samplingFrequency() != frame.samplingFrequency()) {
				b.append(" sampling frequencies differ: last=")
				 .append(ctx.lastFrame.samplingFrequency())
				 .append(" this=")
				 .append(frame.samplingFrequency());
			}
			if (ctx.lastFrame.originality() != frame.originality()) {
				b.append(" originalities differ: last=")
				 .append(ctx.lastFrame.originality())
				 .append(" this=")
				 .append(frame.originality());
			}
			if (ctx.lastFrame.originality() != frame.originality()) {
				b.append(" originalities differ: last=")
				 .append(ctx.lastFrame.originality())
				 .append(" this=")
				 .append(frame.originality());
			}
			if (ctx.lastFrame.home() != frame.home()) {
				b.append(" home indicators differ: last=")
				 .append(ctx.lastFrame.home())
				 .append(" this=")
				 .append(frame.home());
			}
			if (b.length() > 0) {
				rep.carp(ctx.getLocator(), "Stream configuration unexpectedly changed:%s", b.toString());
			}
		}
		if (frame.blockCount() != 0) {
			rep.carp(ctx.getLocator(), "ADTS frame contains more than one AAC frame (supposed compatability issues): %d AAC frames", frame.blockCount()+1);
		}

		// 1 byte of payload might allow an 'END' AAC raw block to appear
		// (even that might be invalid - realistically this should
		// probably be greater than 1; but what?)
		if (frame.payloadLength() < 1) {
			rep.carp(ctx.getLocator(), "ADTS frame too short. A frame_length of "+frame.frameLength()+" gives "+frame.payloadLength()+" bytes of payload.");
		}

		// TODO: check that, if channelConfig == OBJECT_TYPE_SPECIFIC_CONFIG, then AAC really does have a PCE with this info
	}

	@Override
	public ADTSContext createContext(MediaContext parentContext) {
		return new ValidatingADTSContext(parentContext);
	}

}
