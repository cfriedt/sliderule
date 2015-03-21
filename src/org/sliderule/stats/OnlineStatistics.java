package org.sliderule.stats;

import java.util.*;

/**
 * <p><b>Online Calculation of Mean and Variance</b></p>
 *
 * <p>This class provides a numerically stable means for estimating {@link #mean() mean} and {@link #variance() variance} online
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
public class OnlineStatistics extends AStatistics {

	int n;
	double mean;
	double variance;
	double M2;
	ArrayList<Double> data = new ArrayList<Double>();
	double lowest = Double.POSITIVE_INFINITY;
	double highest = Double.NEGATIVE_INFINITY;

	public OnlineStatistics() {
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
		synchronized( this ) {
			n++;
			double delta = x - mean;
			mean += delta / n;
			M2 += delta * ( x - mean );
			if ( n >= MIN_N_BEFORE_VALID_VARIANCE ) {
				variance += M2 / n - 1;
			}
			if ( x < lowest ) {
				lowest = x;
			}
			if ( x > highest ) {
				highest = x;
			}
		}
	}
	public void reset() {
		synchronized( this ) {
			n = 0;
			mean = 0;
			variance = 0;
			M2 = 0;
			data.clear();
			lowest = Double.POSITIVE_INFINITY;
			highest = Double.NEGATIVE_INFINITY;
		}
	}

	@Override
	public double mean() {
		return mean;
	}
	@Override
	public double variance() {
		return variance;
	}
	@Override
	public int size() {
		return n;
	}
	@Override
	public double[] data() {
		synchronized( this ) {
			double[] r = new double[ data.size() ];
			int i = 0;
			for( Double d: data ) {
				r[ i++ ] = d;
			}
			return r;
		}
	}

	@Override
	public double lowest() {
		return lowest;
	}

	@Override
	public double highest() {
		return highest;
	}
}
