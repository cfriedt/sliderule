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
		System.out.println( "" + trial  );
		for( Measurement m: trial.measurements() ) {
			switch( m.description() ) {
//			case "rep":
			case "mean_ns":
			case "variance_ns":
				System.out.println( "" + m );
				break;
			default:
				break;
			}
		}
		System.out.println();
	}
}
