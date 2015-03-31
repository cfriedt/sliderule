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
		r += "size: " + size() + ", ";
		r += "mean: " + mean() + ", ";
//		r += "variance: " + variance() + ", ";
		r += "std: " + standardDeviation() + ", ";
		r += "lowest: " + lowest() + ", ";
		r += "highest: " + highest();
		return r;
	}
}
