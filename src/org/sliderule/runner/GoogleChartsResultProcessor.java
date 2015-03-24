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
import org.sliderule.stats.*;

public class GoogleChartsResultProcessor extends ConsoleResultProcessor {

	public GoogleChartsResultProcessor() {
	}

	@Override
	public void close() throws IOException {
		if ( null != ts.id ) {
			System.out.println( ts );
		}
	}

	@Override
	public void processTrial( Trial trial ) {

		if ( trial.id() != ts.id ) {
			if ( ts.id != null ) {
				System.out.println( ts );
				ts.os.clear();
			}
			ts.id = trial.id();
			ts.proto = trial;
			ts.reps = -1;
		}

		for( Measurement m: trial.measurements() ) {
			switch( m.description() ) {
			case "reps":
				if ( -1 == ts.reps ) {
					ts.reps = (int)m.value().value;
				}
				break;
			case "elapsed_time_ns":
				ts.os.update( (double)(Double) m.value().value );
				break;
			case "warning":
				System.out.println( ts );
				System.out.flush();
				System.err.println( "" + m.value().value );
				System.err.flush();
				ts.id = null;
				ts.os.clear();
				break;
			default:
				break;
			}
		}
	}
}
