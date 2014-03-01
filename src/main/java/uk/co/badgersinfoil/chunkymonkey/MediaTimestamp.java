package uk.co.badgersinfoil.chunkymonkey;

public class MediaTimestamp {
	private long value;
	private MediaUnits units;

	public MediaTimestamp(long value, MediaUnits units) {
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

	public MediaTimestamp plus(MediaDuration duration) {
		if (!units.equals(duration.units())) {
			// TODO: we could attempt conversion, or create some
			// kind of composite object to keep track of durations
			// in each unit separately
			throw new IllegalArgumentException(
				"unit missmatch "+units.unitName()
				+" != "+duration.units().unitName()
			);
		}
		return new MediaTimestamp(value+duration.value(), units);
	}

	public MediaDuration diff(MediaTimestamp other) {
		if (!units.equals(other.units())) {
			throw new IllegalArgumentException(
				"unit missmatch "+units.unitName()
				+" != "+other.units().unitName()
			);
		}
		return new MediaDuration(value-other.value(), units);
	}
}
