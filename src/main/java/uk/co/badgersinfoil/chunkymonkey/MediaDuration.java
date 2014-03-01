package uk.co.badgersinfoil.chunkymonkey;

public class MediaDuration {
	private long value;
	private MediaUnits units;

	public MediaDuration(long value, MediaUnits units) {
		this.value = value;
		this.units = units;
	}
	
	public MediaUnits units() {
		return units;
	}
	public long value() {
		return value;
	}
	
	@Override
	public String toString() {
		return value+" "+units.unitName();
	}

	public MediaDuration plus(MediaDuration duration) {
		if (!units.equals(duration.units())) {
			// TODO: we could attempt conversion, or create some
			// kind of composite object to keep track of durations
			// in each unit separately
			throw new IllegalArgumentException(
				"unit missmatch "+units.unitName()
				+" != "+duration.units().unitName()
			);
		}
		return new MediaDuration(value+duration.value(), units);
	}
}
