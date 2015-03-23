package org.sliderule.runner;

import java.lang.reflect.*;
import java.util.*;

import org.sliderule.model.*;

final class SimpleTrial implements Trial {

	private final Class<?> bench_class;
	private final Method method;
	private final Field[] param;
	private final PolymorphicType[] param_value;

	private final UUID id;
	private final ArrayList<Measurement> measurements;

	public SimpleTrial( UUID id, Class<?> bench_class, Method method, Field[] param, PolymorphicType[] param_value )
	{
		this.bench_class = bench_class;
		this.method = method;
		this.param = param;
		this.param_value = param_value;
		this.id = id;
		measurements = new ArrayList<Measurement>(  );
	}

	@Override
	public UUID id() {
		return id;
	}

	@Override
	public List<Measurement> measurements() {
		return measurements;
	}

	void addMeasurement( Measurement measurement ) {
		measurements.add( measurement );
	}

	@Override
	public String toString() {
		return Algorithm.nameTrial( bench_class, method, param, param_value );
	}
}
