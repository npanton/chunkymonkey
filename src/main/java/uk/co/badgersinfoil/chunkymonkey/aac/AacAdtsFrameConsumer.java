package uk.co.badgersinfoil.chunkymonkey.aac;

/*
import net.sourceforge.jaad.aac.ChannelConfiguration;
import net.sourceforge.jaad.aac.DecoderConfig;
import net.sourceforge.jaad.aac.Profile;
import net.sourceforge.jaad.aac.SampleFrequency;
import net.sourceforge.jaad.aac.syntax.BitStream;
import net.sourceforge.jaad.aac.syntax.Constants;
import net.sourceforge.jaad.aac.syntax.END;
import net.sourceforge.jaad.aac.syntax.Element;
import net.sourceforge.jaad.aac.syntax.ElementListener;
import net.sourceforge.jaad.aac.syntax.FIL;
import net.sourceforge.jaad.aac.syntax.PCE;
import net.sourceforge.jaad.aac.syntax.SyntacticElements;
*/
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.MediaDuration;
import uk.co.badgersinfoil.chunkymonkey.MediaUnits;
import uk.co.badgersinfoil.chunkymonkey.adts.ADTSContext;
import uk.co.badgersinfoil.chunkymonkey.adts.ADTSFrame;
import uk.co.badgersinfoil.chunkymonkey.adts.AdtsFrameConsumer;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter;

public class AacAdtsFrameConsumer implements AdtsFrameConsumer {

	public class AacAdtsContext implements ADTSContext {
		private boolean sbrPresent;
		private MediaDuration duration = null;
		private MediaContext parentContext;

		public AacAdtsContext(MediaContext parentContext) {
			this.parentContext = parentContext;
		}

		@Override
		public MediaDuration getDuration() {
			return duration;
		}

		@Override
		public Locator getLocator() {
			return parentContext.getLocator();
		}
	}

	private Reporter rep = Reporter.NULL;

	@Override
	public void frame(final ADTSContext adtsCtx, final ADTSFrame adtsframe) {
		AacAdtsContext ctx = (AacAdtsContext)adtsCtx;
		ctx.sbrPresent = false;
		// AAC parsing is hard.  Dropped hand-spun code for hacked jaad to get off the ground,
		//AacParser parser = new AacParser(adtsframe.samplingFrequency());
		//parser.parser(adtsframe.payload());
/*
		DecoderConfig config = new DecoderConfig();
		config.setChannelConfiguration(ChannelConfiguration.forInt(adtsframe.channelConfig().getIndex()));
		config.setProfile(Profile.forInt(adtsframe.profile().getIndex()));
		config.setSampleFrequency(SampleFrequency.forInt(adtsframe.samplingFrequency().getIndex()));
		ElementListener elementListener = new ElementListener() {
			@Override
			public void element(Element element) {
				if (element.getType() == Constants.ELEMENT_SCE) {
//					System.out.print("SCE");
				} else if (element.getType() == Constants.ELEMENT_LFE) {
//					System.out.print("LFE");
				} else if (element instanceof FIL) {
					FIL fil = (FIL)element;
//					System.out.print("FIL" + fil.getExtensionTypes());
					if (fil.getExtensionTypes().contains("TYPE_SBR_DATA")) {
						sbrPresent = true;
					}
				} else if (element instanceof PCE) {
					PCE pce = (PCE)element;
					rep.carp(adtsframe.getLocator(), "PCE %s", pce);
				} else {
//					System.out.print(element.getClass().getName().substring(element.getClass().getName().lastIndexOf('.')+1));
				}
				if (element == END.INSTANCE) {
//					System.out.println();
				} else {
//					System.out.print(" ");
				}
			}
		};
*/
//		try {
//			SyntacticElements syn = new SyntacticElements(config);
//			syn.setElementListener(elementListener);
//			syn.decode(new BitStream(adtsframe.payload()));
			// AAC-HE upsamples the core AAC-LC stream,
			// resulting in 2048 samples per
			// frame, but the advertised sampling frequency
			// in ADTS headers represents the pre-upsampling
			// value, so 1024 gives the correct duration
			// calculation even for HE-AAC as long as we
			// just believe the given SR.
			final int NOMINAL_SAMPLES_PER_AAC_FRAME = 1024;
			MediaUnits units = new MediaUnits(NOMINAL_SAMPLES_PER_AAC_FRAME,
			                                  adtsframe.samplingFrequency().getFrequency(),
			                                  "aac-frames");
			MediaDuration frameDuration = new MediaDuration(1, units);
			if (ctx.duration == null) {
				ctx.duration = frameDuration;
			} else {
				ctx.duration = ctx.duration.plus(frameDuration);
			}
//		} catch (Exception e) {
//			rep.carp(ctx.getLocator(), "AAC decode failure: %s", e);
//		};
	}

	public void setReporter(Reporter rep) {
		this.rep = rep;
	}

	@Override
	public ADTSContext createContext(MediaContext parentContext) {
		return new AacAdtsContext(parentContext);
	}
}
