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

class Algorithm {
	private Algorithm() {}
	
	private static class ClassAndInstance {
		public final AnnotatedClass klass;
		public final Object instance;
		public final ArrayList<Trial> trials;
		public ClassAndInstance( AnnotatedClass klass, Object instance ) {
			this.klass = klass;
			this.instance = instance;
			this.trials = new ArrayList<Trial>();
		}
	}

	public static void evaluate( Arguments a, Context c )
	throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException
	{

		ArrayList<ClassAndInstance> alcai = new ArrayList<ClassAndInstance>();

		final int reps = 100;

		System.out.println( "Starting warm-up" );

		for( AnnotatedClass k: c.getAnnotatedClasses() ) {

			Class<?> klazz = k.getAnnotatedClass();
			Object o = klazz.newInstance();

			ClassAndInstance cai = new ClassAndInstance( k, o );
			alcai.add( cai );

			// warm-up microbenchmarks
			for( Method m: k.getBenchmarkMethods() ) {
				m.invoke( o, 10000 );
			}

//			// warm-up macrobenchmarks
//			for( Method m: k.getMacrobenchmarkMethods() ) {
//				m.invoke( o, 10000 );
//			}
		}

		System.out.println( "Finished warm-up" );

		for( ClassAndInstance cai: alcai ) {
			AnnotatedClass k = cai.klass;
			Object o = cai.instance;

			for( Method m: k.getBeforeExperimentMethods() ) {
				m.invoke( o );
			}

			for( Method m: k.getBenchmarkMethods() ) {
				long nanos = 0;
				for( int i=0; i<a.trials; i++ ) {
					long start_nanos = System.nanoTime();
					long l = (long)(Long) m.invoke( o, reps );
					long end_nanos = System.nanoTime();
					nanos += end_nanos - start_nanos + l - l;
				}
				Class<?> klazz = cai.klass.getAnnotatedClass();
				System.out.println( klazz.getName() + "." + m.getName() + "():" );
				System.out.println( "\t" + "trials: " + a.trials );
				System.out.println( "\t" + "reps: " + reps );
				System.out.println( "\t" + "total time (ns): " + nanos );
				System.out.println( "\t" + "average time (ns): " + ( (double)nanos / ( (double)a.trials * reps ) ) );
			}

			for( Method m: k.getAfterExperimentMethods() ) {
				m.invoke( o );
			}
		}
	}
}
