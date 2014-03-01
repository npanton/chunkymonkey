package uk.co.badgersinfoil.chunkymonkey.aac;

public class PredictorInfo {
	public static int getPredSfbMax(SamplingFrequencyIndex i) {
		return new int[] {
			33,
			33,
			38,
			40,
			40,
			40,
			41,
			41,
			37,
			37,
			37,
			34
		}[i.getIndex()];
	}
}
