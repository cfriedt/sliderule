package org.sliderule.runner;

final class Norm {
	private Norm() {}

	static private final double[] p = { 0.005, 0.01, 0.025, 0.05, 0.10, 0.20, 0.25, 0.30, 0.40, 0.50, 0.60, 0.70, 0.75, 0.80, 0.90, 0.95, 0.975, 0.99, 0.995 };
	static private final double[] xarray = { -2.575829, -2.326348, -1.959964, -1.644854, -1.281552, -0.841621, -0.674490, -0.524401, -0.253347, 0.000000, 0.253347, 0.524401, 0.674490, 0.841621, 1.281552, 1.644854, 1.959964, 2.326348, 2.575829, };
	
	/**
	 * <p> Calculate the area under the standard <a href="http://en.wikipedia.org/wiki/Normal_distribution">normal distribution</a>'s probability density function for parameter {@code x}.</p> 
	 * @param x multiple of the standard deviation
	 * @return The area under the standard normal p.d.f. for {@code x} 
	 */
	static double cdf( double x ) {
		double r = 0.0;
		int i=0;
		for( double xx: xarray ) {
			if ( x <= xx ) {
				r = p[i];
			} else {
				break;
			}
		}
		return r;
	}
}
