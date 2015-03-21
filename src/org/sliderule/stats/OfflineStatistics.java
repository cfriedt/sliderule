package org.sliderule.stats;

import java.util.*;

/**
 * <p><b>Offline Calculation of Mean and Variance</b></p>
 *
 * <p>This class provides methods for calculating the {@link #mean()} and {@link #variance()} of
 * sample data, after collection has taken place.
 * </p>
 *
 * @author <a href="mailto:chrisfriedt@gmail.com">Christopher Friedt</a>
 * @see
 *   <ul>
 *     <li>Wikipedia: <a href="http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Online_algorithm">Algorithms for Calculating Variance</a></li>
 *     <li>DeGroot, Morris H., and Schervish, Mark J. Probability and Statistics, 3rd Edition. Toronto: Addison-Wesley, 2002. pp. 429. Print.</li>
 *   </ul>
 */
public class OfflineStatistics extends AStatistics {
	public static final int MIN_N_BEFORE_VALID_VARIANCE = 2;

	final double[] data;
	final double[] ordered_data;
	boolean mean_calculated = false;
	double mean = 0;
	boolean variance_calculated = false;
	double variance = 0;

	public OfflineStatistics( byte[] data ) {
		this.data = new double[ data.length ];
		for( int i=0; i < data.length; i++ ) {
			this.data[ i ] = (double) data[ i ];
		}
		ordered_data = Arrays.copyOf( this.data, this.data.length );
		Arrays.sort( ordered_data );
	}
	public OfflineStatistics( short[] data ) {
		this.data = new double[ data.length ];
		for( int i=0; i < data.length; i++ ) {
			this.data[ i ] = (double) data[ i ];
		}
		ordered_data = Arrays.copyOf( this.data, this.data.length );
		Arrays.sort( ordered_data );
	}
	public OfflineStatistics( int[] data ) {
		this.data = new double[ data.length ];
		for( int i=0; i < data.length; i++ ) {
			this.data[ i ] = (double) data[ i ];
		}
		ordered_data = Arrays.copyOf( this.data, this.data.length );
		Arrays.sort( ordered_data );
	}
	public OfflineStatistics( long[] data ) {
		this.data = new double[ data.length ];
		for( int i=0; i < data.length; i++ ) {
			this.data[ i ] = (double) data[ i ];
		}
		ordered_data = Arrays.copyOf( this.data, this.data.length );
		Arrays.sort( ordered_data );
	}
	public OfflineStatistics( float[] data ) {
		this.data = new double[ data.length ];
		for( int i=0; i < data.length; i++ ) {
			this.data[ i ] = (double) data[ i ];
		}
		ordered_data = Arrays.copyOf( this.data, this.data.length );
		Arrays.sort( ordered_data );
	}
	public OfflineStatistics( double[] data ) {
		this.data = Arrays.copyOf( data, data.length );
		ordered_data = Arrays.copyOf( this.data, this.data.length );
		Arrays.sort( ordered_data );
	}

	@Override
	public double mean() {
		synchronized( this ) {
			if ( ! mean_calculated ) {
				mean = 0;
				for( double d: data ) {
					mean += d;
				}
				mean /= data.length;
				mean_calculated = true;
			}
		}
		return mean;
	}

	@Override
	public double variance() {
		synchronized( this ) {
			double u = mean();
			if ( ! variance_calculated ) {
				variance = 0;
				for( double d: data ) {
					variance += Math.pow( d - u, 2 );
				}
				variance /= data.length;
				variance_calculated = true;
			}
		}
		return variance;
	}

	@Override
	public int size() {
		return data.length;
	}

	@Override
	public double[] data() {
		return Arrays.copyOf( data, data.length );
	}

	@Override
	public double[] orderedData() {
		return Arrays.copyOf( ordered_data, ordered_data.length );
	}

	@Override
	public double lowest() {
		return ordered_data[0];
	}

	@Override
	public double highest() {
		return ordered_data[ ordered_data.length - 1 ];
	}
}
