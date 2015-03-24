/*
 * Copyright (C) 2015 Christopher Friedt <chrisfriedt@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sliderule.stats;

/**
 * This interface is intended to provide a common description of the methods
 * required (but not their implementations) for analyzing samples of a random variable.
 *
 * @author <a href="mailto:chrisfriedt@gmail.com">Christopher Friedt</a>
 * @see
 *   <ul>
 *     <li>Wikipedia: <a href="http://en.wikipedia.org/wiki/Variance">Variance</a></li>
 *     <li>Wikipedia: <a href="http://en.wikipedia.org/wiki/Mean#Arithmetic_mean_.28AM.29">Arithmetic mean</a></li>
 *     <li>DeGroot, Morris H., and Schervish, Mark J. Probability and Statistics, 3rd Edition. Toronto: Addison-Wesley, 2002. pp. 429. Print.</li>
 *   </ul>
 */
public interface IStatistics {
	/**
	 * Return the backing data set that this {@link IStatistic} represents. The data
	 * is returned with the original sample order preserved.
	 * @return the data
	 */
	double[] data();
	/**
	 * The highest valued datum contained within the data set that this {@link IStatistic} represents.
	 * @return the highest value
	 */
	double highest();
	/**
	 * The least valued datum contained within the data set that this {@link IStatistic} represents.
	 * @return the lowest value
	 */
	double lowest();
	/**
	 * Calculate the arithmetic mean of the data set that this {@link IStatistic} represents.
	 * @return the mean
	 */
	double mean();
	/**
	 * Return the backing data set that this {@link IStatistic} represents. The data
	 * is returned sorted from lowest to highest in natural numeric order.
	 * @return the ordered data
	 */
	double[] orderedData();
	/**
	 * The size of the data set.
	 * @return the size
	 */
	int size();
	/**
	 * Calculate the standard deviation (or sample deviation) of the data set that this
	 * {@link IStatistic} represents.
	 * @return the variance
	 */
	double standardDeviation();
	/**
	 * Calculate the variance of the data set that this {@link IStatistic} represents.
	 * @return the variance
	 */
	double variance();
}
