/*
 * Copyright (C) 2015 Christopher Friedt <chrisfriedt@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sliderule.runner;

import java.lang.reflect.*;
import java.util.*;

import org.sliderule.model.*;

final class SimpleTrial implements Trial {

	private final SlideRuleAnnotations ann;
	private final Method method;
	private final boolean micro;
	private final Field[] param;
	private final PolymorphicType[] param_value;

	private final UUID id;
	private final ArrayList<Measurement> measurements;

	public SimpleTrial( UUID id, SlideRuleAnnotations ann, Method method, Field[] param, PolymorphicType[] param_value )
	{
		this.ann = ann;
		this.method = method;
		this.param = param;
		this.param_value = param_value;
		this.id = id;
		measurements = new ArrayList<Measurement>();

		// keep this loop at the end of the constructor!
		for( Method m: ann.getBenchmarkMethods() ) {
			if ( method == m ) {
				micro = true;
				return;
			}
		}
		micro = false;
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

	SlideRuleAnnotations getSlideRuleAnnotations() {
		return ann;
	}

	Method getMethod() {
		return method;
	}

	boolean isMicro() {
		return micro;
	}

	Field[] getParam() {
		return param;
	}

	PolymorphicType[] getParamValue() {
		return param_value;
	}

	@Override
	public String toString() {
		return SimpleTrial.nameTrial( ann.getAnnotatedClass(), method, param, param_value );
	}

	public static String nameTrial( Class<?> bench_class, Method method, Field[] param, PolymorphicType[] param_value ) {
		String r = "";
		r += bench_class.getName() + "." + method.getName() + "()";
		r += PolymorphicType.nameParams( param, param_value );
		return r;
	}

	static double[] extractMeans( ArrayList<Trial> alt ) {
		double[] r = new double[ alt.size() ];
		int i = 0;
		for ( Trial t: alt ) {
			for( Measurement m: t.measurements() ) {
				if ( "elapsed_time_ns".equals( m.description() ) ) {
					r[ i++ ] = (double)(Double) m.value().value;
					break;
				}
			}
		}
		return r;
	}
}
