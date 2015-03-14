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

class Algorithm {
	private Algorithm() {}
	
	public static void evaluate( Arguments a, Context c )
	throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		final int reps = 1000;
		for( AnnotatedClass k: c.getAnnotatedClasses() ) {
			Class<?> klazz = k.getAnnotatedClass();
			Object o = klazz.newInstance();
			for( Method m: k.getBenchmarkMethods() ) {
				long nanos = 0;
				for( int i=0; i<a.trials; i++ ) {
					long start_nanos = System.nanoTime();
					m.invoke( o, reps );
					long end_nanos = System.nanoTime();
					nanos += end_nanos - start_nanos;
				}
				System.out.println( klazz.getName() + "." + m.getName() + "():" );
				System.out.println( "\t" + "trials: " + a.trials );
				System.out.println( "\t" + "reps: " + reps );
				System.out.println( "\t" + "total time (ns): " + nanos );
				System.out.println( "\t" + "average time (ns): " + ( (double)nanos / ( (double)a.trials * reps ) ) );
			}
		}
	}
}
