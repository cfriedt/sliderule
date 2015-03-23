package org.sliderule.runner;

import java.io.*;
import java.util.*;

import org.sliderule.api.*;
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
			case "warning":
				System.out.println( trial_summary );
				System.out.flush();
				System.err.println( "" + m.value().value );
				System.err.flush();
				trial_summary.id = null;
				trial_summary.os.clear();
				break;
			default:
				break;
			}
		}
	}
}
