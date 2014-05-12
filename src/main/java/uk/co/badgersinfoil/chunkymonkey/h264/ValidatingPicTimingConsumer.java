package uk.co.badgersinfoil.chunkymonkey.h264;

import uk.co.badgersinfoil.chunkymonkey.Locator;
import uk.co.badgersinfoil.chunkymonkey.MediaDuration;
import uk.co.badgersinfoil.chunkymonkey.Reporter;
import uk.co.badgersinfoil.chunkymonkey.h264.PicTimingHeader.ClockTimestamp;
import uk.co.badgersinfoil.chunkymonkey.h264.PicTimingSeiConsumer.PicTimingSeiContext;

/**
 * Reports validity problems with each processed PicTimingHeader to the given
 * Reporter instance, and passes the PicTimingHeader to the given delegate
 * PicTimingConsumer instance if there are no serious validity problems.
 */
public class ValidatingPicTimingConsumer implements PicTimingConsumer {

	// TODO: would probably be useful to have a way to be explicitly
	//       notified of discontinuities

	public class ValidationPicTimingContext implements PicTimingContext {

		private PicTimingSeiContext ctx;
		public ClockTimestamp lastTs;
		private PicTimingContext delegateContext;

		public ValidationPicTimingContext(PicTimingSeiContext ctx, PicTimingContext delegateContext) {
			this.ctx = ctx;
			this.delegateContext = delegateContext;
		}

		@Override
		public PicTimingSeiContext getPicTimingSeiContext() {
			return ctx;
		}
	}

	private PicTimingConsumer delegate;
	private Reporter rep;

	public ValidatingPicTimingConsumer(Reporter rep, PicTimingConsumer delegate) {
		this.rep = rep;
		this.delegate = delegate;
	}

	@Override
	public void picTiming(PicTimingContext pctx, PicTimingHeader picTiming) {
		ValidationPicTimingContext ctx = (ValidationPicTimingContext)pctx;
		if (picTiming.picStruct().numClockTS() == -1) {
			rep.carp(getLocator(ctx), "Unknown/reserved pic_struct %s", picTiming.picStruct());
		}
		boolean valid = true;
		if (picTiming.clockTimestamps() != null) {
			ClockTimestamp[] clockTimestamps = picTiming.clockTimestamps();
			for (ClockTimestamp ts : clockTimestamps) {
				if (ts == null) {
					continue;
				}
				boolean validTs = true;
				if (ts.secondsValue() != -1) {
					if (ts.secondsValue() > 59) {
						rep.carp(getLocator(ctx), "Invalid seconds value %d in %s", ts.secondsValue(), ts);
						validTs = false;
					}
					if (ts.minutesValue() != -1) {
						if (ts.minutesValue() > 59) {
							rep.carp(getLocator(ctx), "Invalid minutes value %d in %s", ts.minutesValue(), ts);
							validTs = false;
						}
						if (ts.hoursValue() != -1) {
							if (ts.hoursValue() > 23) {
								rep.carp(getLocator(ctx), "Invalid hours value %d in %s", ts.hoursValue(), ts);
								validTs = false;
							}
						}
					}
				}
				if (ts.countingType() == 0 && ts.timeOffset() != 0) {
					rep.carp(getLocator(ctx), "Non-zero time_offset found while counting_type is 0: %d, in %s", ts.timeOffset(), ts);
				}
				if (validTs) {
					if (ctx.lastTs != null) {
						MediaDuration diff = ctx.lastTs.toClockTimestamp().diff(ts.toClockTimestamp());
						if (!itsAWrap(ctx.lastTs, ts) && Math.abs(diff.toMillis()) > 1000) {
							rep.carp(getLocator(ctx),
							         "Unexpectedly large timestamp change: %,dms (last <%s>, this <%s>)", diff.toMillis(), ctx.lastTs, ts);
						}
					}
					ctx.lastTs = ts;
				} else {
					ctx.lastTs = null;
				}
				valid &= validTs;
			}
		}
		if (valid) {
			delegate.picTiming(ctx.delegateContext, picTiming);
		}
	}

	/**
	 * Tests two consecutive timestamps to see if the time might have
	 * passed the midnight wrapped-around from 23:50 to 00:00 in the
	 * interim.
	 */
	private boolean itsAWrap(ClockTimestamp lastTs, ClockTimestamp ts) {
		return lastTs.hoursValue() == 23 && lastTs.minutesValue() == 59
		      && ts.hoursValue() == 0 && ts.minutesValue() == 0;
	}

	private Locator getLocator(ValidationPicTimingContext ctx) {
		return ctx.getPicTimingSeiContext().getNalUnitContext().getH264Context().getNalUnit().getLocator();
	}

	@Override
	public PicTimingContext createContext(PicTimingSeiContext ctx) {
		return new ValidationPicTimingContext(ctx, delegate.createContext(ctx));
	}
}
