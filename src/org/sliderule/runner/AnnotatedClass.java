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

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

class AnnotatedClass {
	private static final int FIELD_PARAM;

	private static final int METHOD_AFTER_EXPERIMENT;
	private static final int METHOD_BEFORE_EXPERIMENT;
	private static final int METHOD_BENCHMARK;
	private static final int METHOD_AFTER_REP;
	private static final int METHOD_BEFORE_REP;
	private static final int METHOD_MACROBENCHMARK;

	private static final Map<Class<?>,Integer> field_map;
	private static final Map<Class<?>,Integer> method_map;

	static {
		int field=0;
		FIELD_PARAM = field++;

		field_map = new HashMap<Class<?>,Integer>();
		field_map.put( org.sliderule.Param.class, FIELD_PARAM );

		int method=0;
		METHOD_AFTER_EXPERIMENT = method++;
		METHOD_BEFORE_EXPERIMENT = method++;
		METHOD_BENCHMARK = method++;
		METHOD_AFTER_REP = method++;
		METHOD_BEFORE_REP = method++;
		METHOD_MACROBENCHMARK = method++;

		method_map = new HashMap<Class<?>,Integer>();
		method_map.put( org.sliderule.AfterExperiment.class, METHOD_AFTER_EXPERIMENT );
		method_map.put( org.sliderule.BeforeExperiment.class, METHOD_BEFORE_EXPERIMENT );
		method_map.put( org.sliderule.Benchmark.class, METHOD_BENCHMARK );
		method_map.put( org.sliderule.api.AfterRep.class, METHOD_AFTER_REP );
		method_map.put( org.sliderule.api.BeforeRep.class, METHOD_BEFORE_REP );
		method_map.put( org.sliderule.api.Macrobenchmark.class, METHOD_MACROBENCHMARK );
	}

	private final Class<?> klass;
	private final HashSet<Field>[] field_array;
	private final HashSet<Method>[] method_array;

	@SuppressWarnings("unchecked")
	public AnnotatedClass( Class<?> klass ) {
		this.klass = klass;
		field_array = new HashSet[ field_map.size() ];
		for( int i=0; i < field_array.length; i++ ) {
			field_array[i] = new HashSet<Field>();
		}
		method_array = new HashSet[ method_map.size() ];
		for( int i=0; i < method_array.length; i++ ) {
			method_array[i] = new HashSet<Method>();
		}
	}

	public Class<?> getAnnotatedClass() {
		return klass;
	}
	
	@SuppressWarnings("unchecked")
	public void filterField( Field f ) {
		List<Annotation> anna;
		
		for( Map.Entry<Class<?>,Integer> e: field_map.entrySet() ) {
			Class<Annotation> klass = (Class<Annotation>) e.getKey();
			int val = (int)(Integer)e.getValue();
			anna = new ArrayList<Annotation>();
			anna.addAll( Arrays.asList( getAnnotationsByType( f, klass ) ) );
			anna.addAll( Arrays.asList( getDeclaredAnnotationsByType( f, klass ) ) );
			if ( ! anna.isEmpty() ) {
				field_array[ val ].add( f );
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void filterMethod( Method m ) {
		List<Annotation> anna;
		
		for( Map.Entry<Class<?>,Integer> e: method_map.entrySet() ) {
			Class<Annotation> klass = (Class<Annotation>) e.getKey();
			int val = (int)(Integer)e.getValue();
			anna = new ArrayList<Annotation>();
			anna.addAll( Arrays.asList( getAnnotationsByType( m, klass ) ) );
			anna.addAll( Arrays.asList( getDeclaredAnnotationsByType( m, klass ) ) );
			if ( ! anna.isEmpty() ) {
				method_array[ val ].add( m );
			}
		}
	}


	@SuppressWarnings("unchecked")
	private static <T extends Annotation> T[] getAnnotations( boolean declared, Object o, Class<T> klass ) {
		T[] a = null;
		if ( o instanceof Field ) {
			Field f = (Field)o;
			a = (T[]) ( declared ? f.getDeclaredAnnotations() : f.getAnnotations() );
		} else if ( o instanceof Method ) {
			Method m = (Method)o;
			a = (T[]) ( declared ? m.getDeclaredAnnotations() : m.getAnnotations() );
		} else {
			throw new IllegalArgumentException();
		}
		ArrayList<T> ala = new ArrayList<T>();
		for( T an: a ) {
			Class<? extends Annotation> ant = an.annotationType();
			if ( ant == klass ) {
				ala.add( (T) an );
			}
		}
		return ala.toArray( a );
	}
	private static <T extends Annotation> T[] getAnnotationsByType( Object o, Class<T> klass ) {
		return getAnnotations( false, o, klass );
	}
	private static <T extends Annotation> T[] getDeclaredAnnotationsByType( Object o, Class<T> klass ) {
		return getAnnotations( true, o, klass );
	}

	public Set<Field>  getParamFields() {
		return field_array[ FIELD_PARAM ];
	}

	public Set<Method> getAfterExperimentMethods() {
		return method_array[ METHOD_AFTER_EXPERIMENT ];
	}
	public Set<Method> getBeforeExperimentMethods() {
		return method_array[ METHOD_BEFORE_EXPERIMENT ];
	}
	public Set<Method> getBenchmarkMethods() {
		return method_array[ METHOD_BENCHMARK ];
	}
	public Set<Method> getAfterRepMethods() {
		return method_array[ METHOD_AFTER_REP ];
	}
	public Set<Method> getBeforeRepMethods() {
		return method_array[ METHOD_BEFORE_REP ];
	}
	public Set<Method> getMacrobenchmarkMethods() {
		return method_array[ METHOD_MACROBENCHMARK ];
	}
}
