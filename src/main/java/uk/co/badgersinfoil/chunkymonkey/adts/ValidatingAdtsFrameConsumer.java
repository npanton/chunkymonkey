package uk.co.badgersinfoil.chunkymonkey.adts;

import uk.co.badgersinfoil.chunkymonkey.Reporter;

public class ValidatingAdtsFrameConsumer implements AdtsFrameConsumer {
	
	private Reporter rep;
	private ADTSFrame lastFrame = null;

	public ValidatingAdtsFrameConsumer(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public void frame(ADTSFrame frame) {
		if (frame.syncWord() != 0xfff) {
			rep.carp(frame.getLocator(), "Bad sync word 0x%s (expected 0xfff)", Integer.toHexString(frame.syncWord()));
		}
		if (lastFrame != null) {
			// the spec requires the following headers to be the
			// same in each frame of the ADTS stream (other ADTS
			// headers are allowed to vary),
			StringBuilder b = new StringBuilder();
			if (lastFrame.mpegVersion() != frame.mpegVersion()) {
				b.append(" MPEG versions differ: last=")
				 .append(lastFrame.mpegVersion())
				 .append(" this=")
				 .append(frame.mpegVersion());
			}
			if (lastFrame.layer() != frame.layer()) {
				b.append(" MPEG layer identifiers differ: last=")
				 .append(lastFrame.layer())
				 .append(" this=")
				 .append(frame.layer());
			}
			if (lastFrame.crcPresent() != frame.crcPresent()) {
				b.append(" CRC protection precence differs: last=")
				 .append(lastFrame.crcPresent())
				 .append(" this=")
				 .append(frame.crcPresent());
			}
			if (lastFrame.channelConfig() != frame.channelConfig()) {
				b.append(" channel configurations differ: last=")
				 .append(lastFrame.channelConfig())
				 .append(" this=")
				 .append(frame.channelConfig());
			}
			if (lastFrame.profile() != frame.profile()) {
				b.append(" profile differ: last=")
				 .append(lastFrame.profile())
				 .append(" this=")
				 .append(frame.profile());
			}
			if (lastFrame.samplingFrequency() != frame.samplingFrequency()) {
				b.append(" sampling frequencies differ: last=")
				 .append(lastFrame.samplingFrequency())
				 .append(" this=")
				 .append(frame.samplingFrequency());
			}
			if (lastFrame.originality() != frame.originality()) {
				b.append(" originalities differ: last=")
				 .append(lastFrame.originality())
				 .append(" this=")
				 .append(frame.originality());
			}
			if (lastFrame.originality() != frame.originality()) {
				b.append(" originalities differ: last=")
				 .append(lastFrame.originality())
				 .append(" this=")
				 .append(frame.originality());
			}
			if (lastFrame.home() != frame.home()) {
				b.append(" home indicators differ: last=")
				 .append(lastFrame.home())
				 .append(" this=")
				 .append(frame.home());
			}
			if (b.length() > 0) {
				rep.carp(frame.getLocator(), "Stream configuration unexpectedly changed:%s", b.toString());
			}
		}
		if (frame.blockCount() != 0) {
			rep.carp(frame.getLocator(), "ADTS frame contains more than one AAC frame (supposed compatability issues): %d AAC frames", frame.blockCount()+1);
		}

		// 1 byte of payload might allow an 'END' AAC raw block to appear
		// (even that might be invalid - realistically this should
		// probably be greater than 1; but what?)
		if (frame.payloadLength() < 1) {
			rep.carp(frame.getLocator(), "ADTS frame too short. A frame_length of "+frame.frameLength()+" gives "+frame.payloadLength()+" bytes of payload.");
		}

		// TODO: check that, if channelConfig == OBJECT_TYPE_SPECIFIC_CONFIG, then AAC really does have a PCE with this info
	}

}
