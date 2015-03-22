package org.sliderule.stats;

import java.util.*;

public abstract class AStatistics implements IStatistics {

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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		String r = "";
		r += "{ ";
		r += "size: " + size() + ", ";
		r += "mean: " + mean() + ", ";
		r += "variance: " + variance() + ", ";
		r += "std_deviation: " + standardDeviation() + ", ";
		r += "lowest: " + lowest() + ", ";
		r += "highest: " + highest() + ", ";
		r += "}";
		return r;
	}
}
