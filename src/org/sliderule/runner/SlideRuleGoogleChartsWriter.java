package org.sliderule.runner;

import java.io.*;
import java.util.*;

import org.sliderule.model.*;

class SlideRuleGoogleChartsWriter {

	// InMemoryResultProcessor.
	
	final PrintWriter pw;
	final TreeMap<UUID,ArrayList<Trial>> trials;
	final boolean sweep;

	public SlideRuleGoogleChartsWriter( OutputStream os, TreeMap<UUID,ArrayList<Trial>> trials ) {
		this.pw = new PrintWriter( os );
		this.trials = trials;
		sweep = GoogleChartsResultProcessor.trialsAreParametricSweep( trials );
	}

	void writeSimple() {
		
	}

	void writeSweep() {
		
	}

	public void write()
	{
		if ( sweep ) {
			writeSweep();
		} else {
			writeSimple();
		}
	}

	public void close()
	{
		pw.close();
	}
}
