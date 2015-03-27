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

package org.sliderule;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;
import org.sliderule.model.*;
import org.sliderule.runner.*;
import org.sliderule.stats.*;

import examples.*;

public class FactorialTest {

	@BeforeClass
	public static void setup()
	throws Exception
	{
		String[] arg = new String[] {
			//"--debug", "1",
			//"--max-trials", "3",
			"-Cresults.console.class=org.sliderule.runner.InMemoryResultProcessor",
			FactorialBenchmark.class.getName()
		};
		SlideRuleMain.main( arg );
	}

	TreeMap<UUID,ArrayList<Trial>> filterByMethodName( TreeMap<UUID,ArrayList<Trial>> trials, String method_name ) {

		TreeMap<UUID,ArrayList<Trial>> r = new TreeMap<UUID,ArrayList<Trial>>();

		for( Map.Entry<UUID,ArrayList<Trial>> e: trials.entrySet() ) {

			UUID id = e.getKey();
			ArrayList<Trial> alt = e.getValue();

			Trial proto = alt.get( 0 );
			String proto_string = "" + proto;

			if ( proto_string.contains( "." + method_name + "()" )  ) {
				r.put( id, alt );
			}
		}

		return r;
	}

	TreeMap<UUID,ArrayList<Trial>> filterByParams( TreeMap<UUID,ArrayList<Trial>> trials, String param_string ) {

		TreeMap<UUID,ArrayList<Trial>> r = new TreeMap<UUID,ArrayList<Trial>>();

		for( Map.Entry<UUID,ArrayList<Trial>> e: trials.entrySet() ) {

			UUID id = e.getKey();
			ArrayList<Trial> alt = e.getValue();

			Trial proto = alt.get( 0 );
			String proto_string = "" + proto;

			if ( proto_string.contains( "[" + param_string + "]" )  ) {
				r.put( id, alt );
			}
		}

		return r;
	}

	private IStatistics getStats( ArrayList<Trial> trials ) {
		ArrayList<Double> etns = new ArrayList<Double>(); 
		for( Trial t: trials ) {
			for( Measurement m: t.measurements() ) {
				if ( "elapsed_time_ns".equals( m.description() ) ) {
					etns.add( (double) m.value().value );
				}
			}
		}
		OfflineStatistics os = new OfflineStatistics( (double[]) ArrayUtils.toPrimitive( (Double[]) etns.toArray() ) );
		return os;
	}

	@Test
	public void iterativeFasterThanRecursive() {

		TreeMap<UUID,ArrayList<Trial>> trials = InMemoryResultProcessor.trialSet();

		TreeMap<UUID,ArrayList<Trial>> recursive = filterByMethodName( trials, "recursive" );
		TreeMap<UUID,ArrayList<Trial>> iterative = filterByMethodName( trials, "iterative" );

		recursive = filterByParams( recursive, "number:int:5" );
		iterative = filterByParams( iterative, "number:int:5" );

		IStatistics recursive_statistics = getStats( recursive.firstEntry().getValue() );
		IStatistics iterative_statistics = getStats( iterative.firstEntry().getValue() );

		assertEquals( "iterative mean time " + iterative_statistics.mean() + " is less than recursive mean time " + recursive_statistics.mean(), true, iterative_statistics.mean() < recursive_statistics.mean() );
	}
}
