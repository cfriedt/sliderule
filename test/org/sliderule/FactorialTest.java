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

import java.util.List;

import org.junit.*;
import org.sliderule.runner.*;
import org.sliderule.runner.InMemoryResultProcessor.*;

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

	@Test
	public void iterativeFasterThanRecursive() {

		List<TrialSummary> summaries = InMemoryResultProcessor.getTrialSummaries();

		TrialSummary first_recursive = null;
		TrialSummary first_iterative = null;

		for( TrialSummary ts: summaries ) {
			if ( null == first_recursive ) {
				if ( ( "" + ts.proto ).contains( ":recursive" )  ) {
					first_recursive = ts;
					continue;
				}
			}
			if ( null == first_iterative ) {
				if ( ( "" + ts.proto ).contains( ":iterative" )  ) {
					first_iterative = ts;
					continue;
				}
			}
			if ( ! ( null == first_recursive || null == first_iterative ) ) {
				break;
			}
		}
		if ( null == first_recursive || null == first_iterative ) {
			throw new IllegalStateException( "one of the trials is null :(" );
		}
		assertEquals( "iterative mean time " + first_iterative.os.mean() + " is less than recursive mean time " + first_recursive.os.mean(), true, first_iterative.os.mean() < first_recursive.os.mean() );
	}
}
