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
