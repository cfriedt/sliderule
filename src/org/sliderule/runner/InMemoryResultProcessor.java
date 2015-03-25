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

package org.sliderule.runner;

import java.io.*;
import java.util.*;

import org.sliderule.api.*;
import org.sliderule.model.*;

/**
 * <p><b>In-Memory Result Processor</b></p>
 *
 * <p>This class simply stores benchmarking results in a {@link TreeMap}
 * sorted by {@link UUID}.</p>
 *
 * @author <a href="mailto:chrisfriedt@gmail.com">Christopher Friedt</a>
 */
public class InMemoryResultProcessor implements ResultProcessor {

	protected static final TreeMap<UUID,ArrayList<Trial>> trial_set = new TreeMap<UUID,ArrayList<Trial>>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processTrial( Trial trial ) {

		ArrayList<Trial> alt;

		if ( ! trial_set.containsKey( trial.id() ) ) {
			alt = new ArrayList<Trial>();
			trial_set.put( trial.id(), alt );
		}

		alt = trial_set.get( trial.id() );
		alt.add( trial );
	}

	public static TreeMap<UUID,ArrayList<Trial>> trialSet() {
		return trial_set;
	}
}
