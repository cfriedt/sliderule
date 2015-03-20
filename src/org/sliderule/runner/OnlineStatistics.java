package org.sliderule.runner;

import java.util.*;

/**
 * <p><b>Online Calculation of Mean and Variance</b></p>
 *
 * <p>This class provides a numerically stable means for estimating mean and variance online
 * - i.e. iteratively (possibly while the experiment is happening).
 * </p>
 *
 * @author <a href="mailto:chrisfriedt@gmail.com">Christopher Friedt</a>
 * @see
 *   <ul>
 *     <li>Wikipedia: <a href="http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Online_algorithm">Algorithms for Calculating Variance</a></li>
 *     <li>DeGroot, Morris H., and Schervish, Mark J. Probability and Statistics, 3rd Edition. Toronto: Addison-Wesley, 2002. pp. 429. Print.</li>
 *   </ul>
 */
class OnlineStatistics {
	public static final int MIN_N_BEFORE_VALID_VARIANCE = 2;

	private int n;
	private double mean;
	private double variance;
	private double M2;

	OnlineStatistics() {
		reset();
	}

	public void update( byte x ) {
		update( (double) x );
	}
	public void update( short x ) {
		update( (double) x );
	}
	public void update( int x ) {
		update( (double) x );
	}
	public void update( long x ) {
		update( (double) x );
	}
	public void update(  float x ) {
		update( (double) x );
	}
	public void update(  double x ) {
		n++;
		double delta = x - mean;
		mean += delta / n;
		M2 += delta * ( x - mean );
		if ( n >= MIN_N_BEFORE_VALID_VARIANCE ) {
			variance += M2 / n - 1;
		}
	}
	public double getMean() {
		return mean;
	}
	public double getVariance() {
		return variance;
	}
	public int getNumSamples() {
		return n;
	}
	public void reset() {
		n = 0;
		mean = 0;
		variance = 0;
		M2 = 0;
	}
	public boolean accept() {
		return false;
	}
}
