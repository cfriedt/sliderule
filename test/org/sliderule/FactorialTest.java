package org.sliderule;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.*;
import org.sliderule.runner.*;
import org.sliderule.runner.InMemoryResultProcessor.*;

import examples.*;

public class FactorialTest {

	@BeforeClass
	public static void setup() {
		String[] arg = new String[] {
			"-Cresults.console.class=org.sliderule.runner.InMemoryResultProcessor",
			FactorialBenchmark.class.getName()
		};
		SlideRuleMain.main( arg );
	}

	@Test
	public void compareTestResults() {
		List<TrialSummary> summaries = InMemoryResultProcessor.getTrialSummaries();
		TrialSummary first_recursive = null;
		TrialSummary first_iterative = null;
		for( TrialSummary ts: summaries ) {
			if ( null == first_recursive ) {
				if ( ( "" + ts ).contains( ":recursive" )  ) {
					first_recursive = ts;
					continue;
				}
			}
			if ( null == first_iterative ) {
				if ( ( "" + ts ).contains( ":iterative" )  ) {
					first_iterative = ts;
					continue;
				}
			}
			if ( ! ( null == first_recursive || null == first_iterative ) ) {
				break;
			}
		}
		if ( null == first_recursive || null == first_iterative ) {
			throw new IllegalStateException();
		}
		assertEquals( "iterative mean time " + first_iterative.os.mean() + " is less than recursive mean time " + first_recursive.os.mean(), true, first_iterative.os.mean() < first_recursive.os.mean() );
	}
}
