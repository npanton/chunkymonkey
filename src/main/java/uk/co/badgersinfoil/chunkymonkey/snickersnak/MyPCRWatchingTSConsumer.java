package uk.co.badgersinfoil.chunkymonkey.snickersnak;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import uk.co.badgersinfoil.chunkymonkey.MediaDuration;
import uk.co.badgersinfoil.chunkymonkey.ts.TSContext;
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
	private static final SimpleDateFormat format = new SimpleDateFormat("'chunk_'Y-MM-dd'T'hh:mm:ss.SSS");

	public MyPCRWatchingTSConsumer(ChunkingTSPacketConsumer chunker) {
		this.chunker = chunker;
	}

	private void pcr(TSContext ctx, ProgramClockReference programClockReference) {
		if (pcrClockOffset != null) {
//System.out.println("PCR: "+programClockReference.toSexidecimalString());
			final long ts = programClockReference.getPcrBase();
			if (lastPts != -1) {
				long diff = ts - lastPts;
				if (diff < 0) {
System.err.println("!wrap "+diff+": "+lastPts+" -> "+ts);
					pcrClockOffset = pcrClockOffset.plus(WRAP_CORRECTION);
				}
			}
			long corrected = ts + pcrClockOffset.value();
			long chunkId = corrected / chunkDurationPts;
			if (lastChunkId != chunkId) {
System.err.println("*CHUNK* " + pcrClockOffset.value()+" pcr="+ts);
				long scale = MyPicTimingConsumer.PTS_UNITS.rate();
				Calendar c = new GregorianCalendar();
				c.setTimeInMillis(corrected * 1000 / scale);
				chunker.setChunkId(format.format(c.getTime()));
				lastChunkId = chunkId;
			}
			lastPts = ts;
		}
	}

	// TODO: some kind of context argument required?
	public void setPcrClockOffset(MediaDuration pcrClockOffset) {
		this.pcrClockOffset = pcrClockOffset;
	}

	@Override
	public void packet(TSContext ctx, TSPacket pkt) {
		if (pkt.adaptionControl().adaptionFieldPresent()) {
			AdaptationField adaptationField = pkt.getAdaptationField();
			if (adaptationField.pcrFlag()) {
				pcr(ctx, adaptationField.pcr());
				
			}
		}
	}

	@Override
	public void end(TSContext context) {
		// TODO Auto-generated method stub
		
	}
}
