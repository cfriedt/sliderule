package org.sliderule.runner;

import java.io.*;

import org.sliderule.api.*;
import org.sliderule.model.*;

public class ConsoleResultProcessor implements ResultProcessor {

	@Override
	public void close() throws IOException {
	}

	@Override
	public void processTrial( Trial trial ) {
		boolean have_reps = false;
		long reps = -1;
		boolean have_trial_start_ns = false;
		long trial_start_ns = -1;
		boolean have_trial_end_ns = false;
		long trial_end_ns = -1;
		for( Measurement m: trial.measurements() ) {
			switch( m.description() ) {
			case "reps":
				reps = (int) m.value().value;
				have_reps = true;
				break;
			case "trial_start_ns":
				trial_start_ns = (long) m.value().value;
				have_trial_start_ns = true;
				break;
			case "trial_end_ns":
				trial_end_ns = (long) m.value().value;
				have_trial_end_ns = true;
				break;
			default:
				break;
			}
			if ( have_trial_start_ns && have_trial_end_ns && have_reps ) {
				System.out.println( "" + ( (double)( trial_end_ns - trial_start_ns ) / (double) reps ) + "," );
				break;
			}
		}
	}
}
