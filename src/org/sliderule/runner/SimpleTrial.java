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
