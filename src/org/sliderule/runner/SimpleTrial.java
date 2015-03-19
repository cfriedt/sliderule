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

	public SimpleTrial(  Class<?> bench_class, Method method, Field[] param, PolymorphicType[] param_value )
	{
		this.bench_class = bench_class;
		this.method = method;
		this.param = param;
		this.param_value = param_value;
		id = UUID.randomUUID();
		measurements = new ArrayList<Measurement>();
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
		String r = "";
		r += bench_class.getName() + ":" + method.getName() + "():{";
		if ( !( null == param || 0 == param.length ) ) {
			for( int i=0; i<param.length; i++ ) {
				r += param[ i ].getName();
				r += ":";
				r += param_value[ i ];
				if ( i < param.length - 1 ) {
					r += ",";
				}
			}
		}
		r += "}";
		return r;
	}
}
