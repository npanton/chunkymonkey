package uk.co.badgersinfoil.chunkymonkey.snickersnack;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import uk.co.badgersinfoil.chunkymonkey.MediaDuration;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket.AdaptationField;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket.ProgramClockReference;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacketConsumer;

public class MyPCRWatchingTSConsumer implements TSPacketConsumer {

	private ChunkingTSPacketConsumer chunker;
	// TODO: put into a 'context' object,
	private MediaDuration pcrClockOffset;
	private long lastPts = -1;
	private long lastChunkId = -1;
	private int chunkDurationSeconds = 8;
	private long chunkDurationPts = MyPicTimingConsumer.PTS_UNITS.rate() * chunkDurationSeconds;
	private static final MediaDuration WRAP_CORRECTION = new MediaDuration(1L<<32, MyPicTimingConsumer.PTS_UNITS);
	private static final SimpleDateFormat format = new SimpleDateFormat("'chunk_'Y-MM-dd'T'HH:mm:ss.SSS");
	private PesSwitchConsumer h264Switch;
	private long offsetValidityDuration = MyPicTimingConsumer.PTS_UNITS.rate();  // 1 second
	private long offsetValidityEnd = -1;

	public MyPCRWatchingTSConsumer(ChunkingTSPacketConsumer chunker) {
		this.chunker = chunker;
	}

	private void pcr(MediaContext ctx, ProgramClockReference programClockReference) {
		if (pcrClockOffset != null) {
//System.out.println("PCR: "+programClockReference.toSexidecimalString());
			final long ts = programClockReference.getPcrBase();
			if (lastPts != -1) {
				long diff = ts - lastPts;
				if (diff < 0) {
System.err.print("!wrap "+diff+": "+lastPts+" -> "+ts+" - chaning pcrClockOffset from "+pcrClockOffset);
					pcrClockOffset = pcrClockOffset.plus(WRAP_CORRECTION);
System.err.println(" to "+pcrClockOffset);
				}
			}
			long corrected = ts + pcrClockOffset.value();
			long chunkId = corrected / chunkDurationPts;
			if (lastChunkId != chunkId) {
				long scale = MyPicTimingConsumer.PTS_UNITS.rate();
				Calendar c = new GregorianCalendar();
				c.setTimeInMillis(corrected * 1000 / scale);
				chunker.setChunkId(format.format(c.getTime()));
				lastChunkId = chunkId;
			}
			lastPts = ts;
			if (isAfter(ts, offsetValidityEnd)) {
				// start parsing H264 data again, so that we
				// will recalculate 'pcrClockOffset' when the
				// next pic_timing header appears,
				h264Switch.enabled(true);
			}
		}
	}

	/**
	 * Returns true if ts1 is after ts2, assuming that the two timestamps
	 * are relatively close to each other (within a few hours) so that it
	 * can take into account timestamp counter wrap-around.
	 */
	private boolean isAfter(long ts1, long ts2) {
		long diff = ts1 - ts2;
		final long MAX_TS = 1L<<33;
		return diff < 0 || diff > (MAX_TS / 2);
	}

	// TODO: some kind of context argument required?
	public void setPcrClockOffset(MediaDuration pcrClockOffset) {
		this.pcrClockOffset = pcrClockOffset;
		if (lastPts != -1) {
			// stop paying the cost of H264 parsing for a little while,
			h264Switch.enabled(false);
			offsetValidityEnd = lastPts + offsetValidityDuration;
		}
	}

	@Override
	public void packet(MediaContext ctx, TSPacket pkt) {
		if (pkt.adaptionControl().adaptionFieldPresent()) {
			AdaptationField adaptationField = pkt.getAdaptationField();
			if (adaptationField.pcrFlag()) {
				pcr(ctx, adaptationField.pcr());
				
			}
		}
	}

	@Override
	public void end(MediaContext context) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Specifies the PesSwitchConsumer that allows this object to enable
	 * and disable parsing of the H264 elementry stream in order to create
	 * an association between H264 pic_timing values, and PCR values, or
	 * to validate that a previously calculated association is still valid.
	 */
	public void setH264Switch(PesSwitchConsumer h264Switch) {
		this.h264Switch = h264Switch;
		h264Switch.enabled(true);
	}

	@Override
	public MediaContext createContext(MediaContext parent) {
		// TODO Auto-generated method stub
		return null;
	}
}
