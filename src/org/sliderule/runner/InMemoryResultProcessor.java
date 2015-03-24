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

import java.util.*;

import org.sliderule.model.*;
import org.sliderule.stats.*;

public class InMemoryResultProcessor extends ConsoleResultProcessor {

	public static class TrialSummary {
		public UUID id;
		public Trial proto;
		public OnlineStatistics os;
		public TrialSummary() {
			os = new OnlineStatistics();
		}
	}

	private static final ArrayList<TrialSummary> alts;
	private static TrialSummary trial_summary;
	static {
		 alts = new ArrayList<TrialSummary>();
		 trial_summary = new TrialSummary();
		 alts.add( trial_summary );
	}

	public static List<TrialSummary> getTrialSummaries() {
		return alts;
	}

	@Override
	public void processTrial( Trial trial ) {

		super.processTrial( trial );

		if ( trial.id() != trial_summary.id ) {
			if ( trial_summary.id != null ) {
				trial_summary = new TrialSummary();
				alts.add( trial_summary );
			}
			trial_summary.id = trial.id();
			trial_summary.proto = trial;
		}

		for( Measurement m: trial.measurements() ) {
			switch( m.description() ) {
			case "elapsed_time_ns":
				trial_summary.os.update( (double)(Double) m.value().value );
				break;
			default:
				break;
			}
		}
	}
}
