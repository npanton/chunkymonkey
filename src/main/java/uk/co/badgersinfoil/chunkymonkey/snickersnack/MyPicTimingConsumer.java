package uk.co.badgersinfoil.chunkymonkey.snickersnack;

import java.util.Calendar;
import java.util.GregorianCalendar;
import uk.co.badgersinfoil.chunkymonkey.MediaDuration;
import uk.co.badgersinfoil.chunkymonkey.MediaTimestamp;
import uk.co.badgersinfoil.chunkymonkey.MediaUnits;
import uk.co.badgersinfoil.chunkymonkey.h264.PicTimingConsumer;
import uk.co.badgersinfoil.chunkymonkey.h264.PicTimingHeader;
import uk.co.badgersinfoil.chunkymonkey.h264.PicTimingSeiConsumer.PicTimingSeiContext;
import uk.co.badgersinfoil.chunkymonkey.h264.SeqParamSet;
import uk.co.badgersinfoil.chunkymonkey.h264.PicTimingHeader.ClockTimestamp;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket.Parsed;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket.Timestamp;

public class MyPicTimingConsumer implements PicTimingConsumer {

	public class MyPicTimingContext implements PicTimingContext {

		private PicTimingSeiContext ctx;
		long lastPts;
		MediaTimestamp lastClockTimestamp;

		public MyPicTimingContext(PicTimingSeiContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public PicTimingSeiContext getPicTimingSeiContext() {
			return ctx;
		}
	}

	public static final MediaUnits PTS_UNITS = new MediaUnits(1, 90000, "PTS_ticks");
	private MyPCRWatchingTSConsumer pcrWatcher;

	public MyPicTimingConsumer(MyPCRWatchingTSConsumer pcrWatcher) {
		this.pcrWatcher = pcrWatcher;
	}

	@Override
	public void picTiming(PicTimingContext pctx, PicTimingHeader picTiming) {
		MyPicTimingContext ctx = (MyPicTimingContext)pctx;
		ClockTimestamp[] timestamps = picTiming.clockTimestamps();
		MediaTimestamp clockTimestamp = null;
		MediaTimestamp expanded = null;
		if (timestamps != null && timestamps.length > 0) {
			ClockTimestamp ts = timestamps[0];
			if (ts != null) {
				clockTimestamp = ts.toClockTimestamp();
				Calendar calendar = new GregorianCalendar();
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);
				long timeAsofMidnight = calendar.getTimeInMillis();
				SeqParamSet seqParamSet = ctx.getPicTimingSeiContext().getNalUnitContext().getH264Context().lastSeqParamSet();
				long midnightTimestamp = timeAsofMidnight * seqParamSet.vuiParameters().timingInfo().timeScale() / 1000 / 2;
				MediaDuration offset = new MediaDuration(midnightTimestamp, clockTimestamp.units());
				// FIXME: must accommodate skew!
				expanded = clockTimestamp.plus(offset);
//System.out.println("expanded:"+expanded+" - "+ts.hoursValue()+":"+ts.minutesValue()+":"+ts.secondsValue()+"."+ts.nFrames()+"+"+ts.timeOffset());
			}
		}
		PESPacket pesPacket = ctx.getPicTimingSeiContext().getNalUnitContext().getH264Context().getPesPacket();
		if (pesPacket.isParsed()) {
			Parsed payload = pesPacket.getParsedPESPaload();
			if (payload.ptsDdsFlags().isPtsPresent()) {
				Timestamp pts = payload.pts();
				long dPts = 0;
				MediaTimestamp ptsTs = null;
				if (pts.isValid()) {
					dPts = pts.getTs() - ctx.lastPts;
					ctx.lastPts = pts.getTs();
					ptsTs = new MediaTimestamp(pts.getTs(), PTS_UNITS);
				}
//System.out.println("PTS: "+pts.toSexidecimal()+" ("+pts.getTs()+" d="+dPts +") hex="+Long.toHexString(pts.getTs()));
				if (clockTimestamp != null) {
//					System.out.print(clockTimestamp);
					if (ctx.lastClockTimestamp != null) {
						MediaDuration dClockTs = clockTimestamp.diff(ctx.lastClockTimestamp);
//						System.out.print(" (d="+dClockTs+")");
					}
				}
				ctx.lastClockTimestamp = clockTimestamp;
//				System.out.println();
				if (expanded != null) {
					MediaTimestamp convert = PTS_UNITS.convert(expanded);
					pcrWatcher.setPcrClockOffset(convert.diff(ptsTs));
//System.out.println("offset: "+convert.diff(ptsTs));
				}
			}
		} else {
			System.err.println("PES packet[stream_id="+pesPacket.streamId()+"] not parsed (trying to get a PTS)\n  at "+pesPacket.getLocator());
		}
	}

	@Override
	public PicTimingContext createContext(PicTimingSeiContext ctx) {
		return new MyPicTimingContext(ctx);
	}

}
