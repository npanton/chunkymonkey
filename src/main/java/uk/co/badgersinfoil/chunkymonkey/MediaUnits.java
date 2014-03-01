package uk.co.badgersinfoil.chunkymonkey;

public class MediaUnits {
	private long base;
	private long scale;
	private String unitName;
	
	public MediaUnits(long base, long scale, String unitName) {
		this.base = base;
		this.scale = scale;
		this.unitName = unitName;
	}

	public long toMillies(long value) {
		return value * base * 1000 / scale;
	}
	public long rate() {
		return scale;
	}
	public long base() {
		return base;
	}
	public String unitName() {
		return unitName;
	}
	public MediaTimestamp convert(MediaTimestamp ts) {
		MediaUnits otheru = ts.units();
//		MediaUnits units = new MediaUnits(scale * otheru.base, base * otheru.scale, unitName);
		return new MediaTimestamp(ts.value()*scale * otheru.base/(base * otheru.scale), this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int)(base ^ (base >>> 32));
		result = prime * result + (int)(scale ^ (scale >>> 32));
		result = prime
				* result
				+ ((unitName == null) ? 0 : unitName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MediaUnits other = (MediaUnits)obj;
		if (base != other.base)
			return false;
		if (scale != other.scale)
			return false;
		if (unitName == null) {
			if (other.unitName != null)
				return false;
		} else if (!unitName.equals(other.unitName))
			return false;
		return true;
	}
}