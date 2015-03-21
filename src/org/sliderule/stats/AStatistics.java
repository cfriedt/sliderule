package org.sliderule.stats;

import java.util.*;

abstract class AStatistics implements IStatistics {

	public static final int MIN_N_BEFORE_VALID_VARIANCE = 2;

	@Override
	final public double standardDeviation() {
		return Math.sqrt( variance() );
	}
	@Override
	public double[] orderedData() {
		synchronized( this ) {
			double[] data = data();
			Arrays.sort( data );
			return data;
		}
	}

	@Override
	public String toString() {
		String r = super.toString() + ":{ " +
			"mean: " + mean() + ", " +
			"variance: " + variance() + ", " +
			"}";
		return r;
	}
}
