package org.sliderule.stats;

import java.util.*;

/**
 * This class provides methods to {@link #generate} a {@link Histogram} based on a given number, or a guessed number of suitable {@link #partition partitions}.
 *
 * @author <a href="mailto:chrisfriedt@gmail.com">Christopher Friedt</a>
 * @see
 *   <ul>
 *     <li>Wikipedia: <a href="http://en.wikipedia.org/wiki/Histogram">Histogram</a></li>
 *     <li>DeGroot, Morris H., and Schervish, Mark J. Probability and Statistics, 3rd Ed. Toronto: Addison-Wesley, 2002. pp. 776-778. Print.</li>
 *     <li>Wikipedia: <a href="http://en.wikipedia.org/wiki/Histogram#Number_of_bins_and_width">Scott's normal reference rule</a></li>
 *   </ul>
 */
public final class Histogram {
	final IStatistics is;
	final double[] data;
	final double[] normalized_data;
	final double bin_width;
	final double[] bin_centers;

	/**
	 * Generate a {@link Histogram} representative of the provided data set.
	 * @param n generate a {@link Histogram} with {@code n} bins.
	 * @param data the set of data in question
	 * @return the {@link Histogram}
	 */
	public Histogram( int n, double[] data ) {
		this( n, new OfflineStatistics( data ) );
	}
	/**
	 * Generate a {@link Histogram} representative of the provided data set.
	 * The generated {@link Histogram} will have optimum {@link #binWidth() bin width}.
	 * @param data the set of data in question
	 * @return the {@link Histogram}
	 */
	public Histogram( double[] data ) {
		this( new OfflineStatistics( data ) );
	}
	/**
	 * Generate a {@link Histogram} representative of the provided data set.
	 * The generated {@link Histogram} will have optimum {@link #binWidth() bin width}.
	 * @param is the {@link IStatistics} representing the data set in question
	 * @return the {@link Histogram}
	 */
	public Histogram( IStatistics is ) {
		this( partition( is ), is );
	}
	/**
	 * Generate a {@link Histogram} representative of the provided data set.
	 * @param n generate a {@link Histogram} with {@code n} bins.
	 * @param is the {@link IStatistics} representing the data set in question
	 * @return the {@link Histogram}
	 */
	public Histogram( int n, IStatistics is ) {
		int dl = is.size();
		double[] ordered_data = is.orderedData();
		double highest = is.highest();
		double lowest = is.lowest();
		double bin_width = ( highest - lowest ) / n;
		double left_side = lowest;
		double right_side = highest;
		double[] bin_centers = new double[ n ];
		data = new double[ bin_centers.length ];
		normalized_data = new double[ bin_centers.length ];

		double center;
		int i, j;

		for( i = 0, center = left_side + bin_width/2; i < bin_centers.length; bin_centers[ i ] = center, center += bin_width, i++ );

		for(
			i=0, j=0, right_side = left_side + bin_width;
			i < n && j < dl;
			i++, left_side += bin_width, right_side += bin_width
		) {
			for( ; j < dl && ordered_data[ j ] <= right_side; data[ i ]++, j++ );
		}

		for( i = 0; i < normalized_data.length; i++ ) {
			normalized_data[ i ] = data[ i ] / dl;
		}

		this.is = is;
		this.bin_width = bin_width;
		this.bin_centers = bin_centers;
	}

	/**
	 * Calculate the bin width that is optimal for random samples of normally distributed data.
	 * The returned bin width is optimal in the
	 * <a href="http://en.wikipedia.org/wiki/Minimum_mean_square_error">MMSE</a>
	 * sense.
	 * @param n number of samples
	 * @param o sample standard deviation
	 * @return bin width
	 */
	public static double binWidth( int n, double o ) {
		return 3.5 * o / Math.pow( n, 1 / 3D );
	}

	/**
	 * Determine an optimum number of partitions such that the {@link #binWidth() bin width}
	 * is optimal for a set of data.
	 * @param n number of samples in the data set
	 * @param o sample standard deviation of the data set
	 * @param lowest the lowest datum measured in the data set
	 * @param highest the highest datum measured in the data set
	 * @return the number of partitions
	 * @see {@link #partition(double[])}
	 * @see {@link #partition(IStatistics)}
	 */
	public static int partition( int n, double o, double lowest, double highest ) {
		if ( n < AStatistics.MIN_N_BEFORE_VALID_VARIANCE ) {
			throw new IllegalArgumentException();
		}
		if ( highest < lowest ) {
			throw new IllegalArgumentException();
		}
		return (int) ( Math.ceil( highest - lowest ) / binWidth( n, o ) );
	}
	/**
	 * Determine an optimum number of partitions such that the {@link #binWidth() bin width}
	 * is optimal for a set of data.
	 * @param is the {@link IStatistics} representing the data set in question
	 * @return the number of partitions
	 * @see {@link #partition(int, double, double, double)}
	 * @see {@link #partition(double[])}
	 */
	public static int partition( IStatistics is ) {
		return partition( is.size(), is.standardDeviation(), is.lowest(), is.highest() );
	}
	/**
	 * Determine an optimum number of partitions such that the {@link #binWidth() bin width}
	 * is optimal for a set of data.
	 * @param data the set of data in question
	 * @return the number of partitions
	 * @see {@link #partition(int, double, double, double)}
	 * @see {@link #partition(IStatistics)}
	 */
	public static int partition( double[] data ) {
		OfflineStatistics os = new OfflineStatistics( data );
		return partition( os );
	}
	/**
	 * Bin width of the {@link Histogram}.
	 * @return the bin width
	 */
	public double binWidth() {
		return bin_width;
	}
	/**
	 * Returns the center of the {@code i}<sup>th</sup> bin.
	 * @param i the bin number
	 * @return the bin center
	 */
	public double binCenter( int i ) {
		return bin_centers[ i ];
	}
	/**
	 * Returns the center of the all bins.
	 * @param i the bin number
	 * @return the bin center
	 */
	public double[] binCenters() {
		return Arrays.copyOf( bin_centers, bin_centers.length );
	}
	/**
	 * The histogram data.
	 * @return the histogram data.
	 */
	public double[] data() {
		return Arrays.copyOf( data, data.length );
	}
	/**
	 * The histogram data.
	 * @return the histogram data.
	 */
	public double[] normalizedData() {
		return Arrays.copyOf( normalized_data, normalized_data.length );
	}

	/**
	 * The number of bins in the histogram.
	 * @return the number of bins
	 * @throws NoSuchElementException if the value x is not found within the bounds of any bin.
	 */
	public int size() {
		return data.length;
	}
	public int bin( double x ) {
		for( int i = 0; i < bin_centers.length; i++ ) {
			double L = bin_centers[ i ] - bin_width;
			double U = bin_centers[ i ] + bin_width;
			if ( x >= L && x <= U ) {
				return i;
			}
		}
		throw new NoSuchElementException();
	}
	/**
	 * Return the bin number containing the arithmetic mean of the backing data set.
	 * @return the bin number
	 * @throws NoSuchElementException if the value x is not found within the bounds of any bin.
	 */
	public int meanBin() {
		return bin( is.mean() );
	}
}
