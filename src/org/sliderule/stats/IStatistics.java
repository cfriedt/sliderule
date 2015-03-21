package org.sliderule.stats;

public interface IStatistics {
	double mean();
	double variance();
	double standardDeviation();
	int size();
	double[] data();
	double[] orderedData();
	double lowest();
	double highest();
}
