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

import org.sliderule.*;
import org.sliderule.model.*;

class Algorithm {
	
	private static final int N_WARMUP_REPS = 10000;
	
	final Arguments arguments;
	final Context context;
	final ArrayList<ClassAndInstance> alcai;
	final ArrayList<ArrayList<PolymorphicType>> param_values;
	
	private Algorithm( Arguments arguments, Context context ) {
		this.arguments = arguments;
		this.context = context;
		this.alcai = new ArrayList<ClassAndInstance>();
		this.param_values = new ArrayList<ArrayList<PolymorphicType>>();
	}
	
	static class ClassAndInstance {
		public final AnnotatedClass klass;
		public final Object instance;
		public final ArrayList<Trial> trials;
		public ClassAndInstance( AnnotatedClass klass, Object instance ) {
			this.klass = klass;
			this.instance = instance;
			this.trials = new ArrayList<Trial>();
		}
	}
	
	static int factorial( int x ) {
		int result = 1;
		if ( x < 0 ) {
			throw new IllegalArgumentException();
		}
		for( ; x > 0; result *= x, x-- );
		return result;
	}
	private void setup()
	throws InstantiationException, IllegalAccessException
	{
		for( AnnotatedClass ac: context.getAnnotatedClasses() ) {
			Class<?> an = ac.getAnnotatedClass();
			Object o = an.newInstance();
			ClassAndInstance cai = new ClassAndInstance( ac, o );
			alcai.add( cai );
		}
	}
	private void permute() {
		Permute.permute();
	}

	private void warmUp()
	throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		Object r;
		for( ClassAndInstance cai: alcai ) {
			AnnotatedClass k = cai.klass;
			Object o = cai.instance;
			for( Method m: k.getBenchmarkMethods() ) {
				r = m.invoke( o, N_WARMUP_REPS );
			}
			for( Method m: k.getMacrobenchmarkMethods() ) {
				for( int i=0; i<N_WARMUP_REPS; i++ ) {
					r = m.invoke( o );
				}
			}
		}
	}
	
	private void bench() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		for( ClassAndInstance cai: alcai ) {
			AnnotatedClass k = cai.klass;
			Object o = cai.instance;

			for( Method m: k.getBeforeExperimentMethods() ) {
				m.invoke( o );
			}

			for( Method m: k.getBenchmarkMethods() ) {
				
//				double mean = 0;
//				double variance = 0;
//				
//				long nanos = 0;
//				for( int i=0; i<arguments.trials; i++ ) {
//					long start_nanos = System.nanoTime();
//					long l = (long)(Long) m.invoke( o, reps );
//					long end_nanos = System.nanoTime();
//					nanos += end_nanos - start_nanos + l - l;
//				}
//				Class<?> klazz = cai.klass.getAnnotatedClass();
//				System.out.println( klazz.getName() + "." + m.getName() + "():" );
//				System.out.println( "\t" + "trials: " + a.trials );
//				System.out.println( "\t" + "reps: " + reps );
//				System.out.println( "\t" + "total time (ns): " + nanos );
//				System.out.println( "\t" + "average time (ns): " + ( (double)nanos / ( (double)a.trials * reps ) ) );
			}

			for( Method m: k.getAfterExperimentMethods() ) {
				m.invoke( o );
			}
		}
	}
	
	public static void evaluate( Arguments a, Context c )
	throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException
	{
		if ( ! a.bench_classes.isEmpty() ) {
			Algorithm algo = new Algorithm( a, c );
			algo.setup();
			algo.warmUp();
			algo.permute();
			algo.bench();
		}
	}
}
