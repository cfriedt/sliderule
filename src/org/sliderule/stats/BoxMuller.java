package org.sliderule.stats;

/**
 * <p><b>Box-Muller Transform</b></p>
 *
 * <p>This class provides a
 * <a href="http://en.wikipedia.org/wiki/Pseudorandom_number_generator">pseudo-random number generator (PRNG)</a>
 * based on the
 * <a href="http://en.wikipedia.org/wiki/Box-Muller_transform">Box-Muller transform</a>.
 * The transform is used to convert samples from a
 * <a href="http://en.wikipedia.org/wiki/Uniform_distribution_(continuous)">uniformly</a>
 * distributed random variable to samples from a {@link Normal normally} distributed random variable.
 * </p>
 *
 * @author <a href="mailto:chrisfriedt@gmail.com">Christopher Friedt</a>
 * @see
 *   <ul>
 *     <li>Wikipedia: <a href="http://en.wikipedia.org/wiki/Box-Muller_transform">Box-Muller Transform</a></li>
 *   </ul>
 *
 */
public class BoxMuller {

	private static final double TWO_PI = 2 * Math.PI;
	private static double z0 = -1, z1 = -1;
	private static boolean generate = false;

	public static double random( double u, double o ) {
		generate = !generate;
		if ( !generate ) {
			return z1 * o + u;
		}
		double u1, u2;
		do {
			u1 = Math.random();
			u2 = Math.random();
		} while( u1 <= Double.MIN_NORMAL );
		z0 = Math.sqrt( -2.0 * Math.log( u1 ) ) * Math.cos( TWO_PI * u2 );
		z1 = Math.sqrt( -2.0 * Math.log( u1 ) ) * Math.cos( TWO_PI * u2 );
		return z0 * o + u;
	}
}
