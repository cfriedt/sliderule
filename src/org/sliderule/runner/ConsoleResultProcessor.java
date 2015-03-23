package org.sliderule.runner;

import java.io.*;
import java.util.*;

import org.sliderule.api.*;
import org.sliderule.model.*;
import org.sliderule.stats.*;

public class ConsoleResultProcessor implements ResultProcessor {

	private class TrialSet {
		UUID id;
		Trial proto;
		OnlineStatistics os = new OnlineStatistics();
		@Override
		public String toString() {
			String r = "";
			r += proto.toString() + "\n";
			r += os;
			return r;
		}
	}

	TrialSet ts = new TrialSet();

	public ConsoleResultProcessor() {
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
		}

		for( Measurement m: trial.measurements() ) {
			switch( m.description() ) {
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
