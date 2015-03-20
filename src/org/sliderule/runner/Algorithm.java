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

import org.sliderule.*;
import org.sliderule.api.*;
import org.sliderule.model.*;

class Algorithm {

	final Arguments arguments;
	final Context context;
	final ArrayList<ClassAndInstance> alcai;
	Field[] param_fields;
	PolymorphicType[][] param_values;

	private Algorithm( Arguments arguments, Context context ) {
		this.arguments = arguments;
		this.context = context;
		this.alcai = new ArrayList<ClassAndInstance>();
	}

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

	private static boolean verbose = true;
	private static void D( Object o ) {
		if ( verbose ) {
			System.out.println( "" + o );
		}
	}

/*############################################################################
 *                         Setup Code
 *############################################################################*/

	static int euprod( int[] cardinality ) {
		return euprod( cardinality, -1 );
	}
	static int euprod( int[] cardinality, int index_to_omit ) {
		int result = 1;
		for( int i=0; i < cardinality.length; result *= ( ( i <= index_to_omit || -1 == index_to_omit ) ? 1 : cardinality[ i ] ), i++ );
		return result;
	}

	@SuppressWarnings("unchecked")
	private void permute()
	throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		int np = param_fields.length;

		Class<?>[] type = new Class<?>[ np ];
		ArrayList<PolymorphicType>[] values = new ArrayList[ np ];
		int[] cardinality = new int[ np ];

		// calculate the number of permutations, record cardinality of each param
		for( int i=0; i < np; i++ ) {

			Field f = param_fields[ i ];

			type[ i ] = f.getType();

			Param param = f.getAnnotation( org.sliderule.Param.class );
			List<String> value_string = Arrays.asList( param.value() );
			values[ i ] = new ArrayList<PolymorphicType>();
			for( String s: value_string ) {
				PolymorphicType pmt = PolymorphicType.infer( type[ i ], s );
				values[ i ].add( pmt );
			}
			cardinality[ i ] = value_string.size();
		}

		int nrows = euprod( cardinality );
		int ncols = cardinality.length;
		param_values = new PolymorphicType[ nrows ][ ncols ];

		for( int col=0; col < ncols; col++ ) {
			int val = 0;
			int reps = euprod( cardinality, col );
			for( int row=0; row < nrows; row++ ) {
				param_values[ row ][ col ] = values[ col ].get( val );
				if ( 0 == ( (row+1) % reps ) ) {
					val++;
					val %= cardinality[ col ];
				}
			}
		}
	}

	private void setup()
	throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		for( AnnotatedClass ac: context.getAnnotatedClasses() ) {
			Class<?> an = ac.getAnnotatedClass();
			Object o = an.newInstance();
			ClassAndInstance cai = new ClassAndInstance( ac, o );
			alcai.add( cai );
		}

		AnnotatedClass proto;
		proto = alcai.get( 0 ).klass;
		ArrayList<Field> alf = new ArrayList<Field>();
		alf.addAll( proto.getParamFields() );
		param_fields = alf.toArray( new Field[0] );

		// generate the euclidian parameter space
		permute();
	}

/*############################################################################
 *                         Warm-up the JVM (trigger JIT)
 *############################################################################*/

	private static final int N_WARMUP_REPS;
	private static final int N_MACRO_WARMUP_REPS = 10;

	static {
		int n_warmup_reps = 10;
		try {
			if ( VM.getUseCompiler() ) {
				n_warmup_reps += VM.getCompileThreshold();
				if ( VM.getTieredCompilation() ) {
					n_warmup_reps += VM.getTier2CompileThreshold();
				}
			}
			if ( VM.getInline() ) {
				n_warmup_reps += VM.getMinInliningThreshold();
			}
		} catch ( Throwable t ) {
		}
		N_WARMUP_REPS = n_warmup_reps;
	}

	private void warmUp()
	throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		D( "starting warm-up" );

		Object r;
		for( ClassAndInstance cai: alcai ) {

			AnnotatedClass k = cai.klass;
			Object o = cai.instance;

			for( int row=0; row < param_values.length; row++ ) {

				// set all parameters for a specific trial
				for( int col=0; col < param_values[ row ].length; col++ ) {
					Field f = param_fields[ col ];
					PolymorphicType pmt = param_values[ row ][ col ];
					f.set( o, pmt.value );
				}

				// execute the MicroBenchmarks
//				for( Method m: k.getBenchmarkMethods() ) {
//					r = m.invoke( o, N_WARMUP_REPS );
//				}

				// execute the MacroBenchmarks
				for( Method m: k.getMacrobenchmarkMethods() ) {
					for( int i=0; i<N_MACRO_WARMUP_REPS; i++ ) {
						r = m.invoke( o );
					}
				}
			}
		}

		D( "finished warm-up\n" );
	}

/*############################################################################
 *                         Execute Benchmarks
 *############################################################################*/

	private static long elapsed( long start, long end ) {
		return end - start;
	}

	private void markMacro( AnnotatedClass k, Object o, Method m, int param_set )
	throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		//throw new UnsupportedOperationException();
	}

	private void markMicro( AnnotatedClass k, Object o, Method m, int param_set )
	throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		long trial_start_ns;
		long trial_end_ns;
		long trial_start_ms;

		int n = 0;
		double mean_ns = 0;
		double variance_ns = 0;
		double M2 = 0;

		ArrayList<Trial> trials = new ArrayList<Trial>();

		// warm-up
		m.invoke( o, N_WARMUP_REPS );

		for( int reps = 32, trial = 0; reps < 10000000 && trial < arguments.trials; reps <<= 1, trial++ ) {

			SimpleTrial st = new SimpleTrial( k.getAnnotatedClass(), m, param_fields, param_values[ param_set ] );

			for( Method b4: k.getBeforeRepMethods() ) {
				b4.invoke( o );
			}

			trial_start_ms = System.currentTimeMillis();
			trial_start_ns = System.nanoTime();

			m.invoke( o, reps );

			trial_end_ns = System.nanoTime();

			for( Method aft: k.getAfterRepMethods() ) {
				aft.invoke( o );
			}

			// statistical calculations derived from "numerically stable algorithm"
			// http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Online_algorithm
			n += 1;
			long x = elapsed( trial_start_ns, trial_end_ns );
			double delta = ( (double) x ) - mean_ns;
			mean_ns += delta / n;
			M2 += delta * ( (double)x - mean_ns );
			if ( n >= 2 ) {
				variance_ns += M2 / n - 1;
			}

			SimpleMeasurement mean_ns_measurement = new SimpleMeasurement( "mean_ns", new PolymorphicType( double.class, mean_ns ) );
			SimpleMeasurement variance_ns_measurement = new SimpleMeasurement( "variance_ns", new PolymorphicType( double.class, variance_ns ) );

			st.addMeasurement( mean_ns_measurement );
			st.addMeasurement( variance_ns_measurement );

			SimpleMeasurement rep_measurement = new SimpleMeasurement( "reps", new PolymorphicType( int.class, reps ) );
			SimpleMeasurement trial_start_ms_measurement = new SimpleMeasurement( "trial_start_ms", new PolymorphicType( long.class, trial_start_ms ) );
			SimpleMeasurement trial_start_ns_measurement = new SimpleMeasurement( "trial_start_ns", new PolymorphicType( long.class, trial_start_ns ) );
			SimpleMeasurement trial_end_ns_measurement = new SimpleMeasurement( "trial_end_ns", new PolymorphicType( long.class, trial_end_ns ) );

			st.addMeasurement( rep_measurement );
			st.addMeasurement( trial_start_ms_measurement );
			st.addMeasurement( trial_start_ns_measurement );
			st.addMeasurement( trial_end_ns_measurement );

			context.results_processor.processTrial( st );

			trials.add( st );
		}
	}

	private void mark( boolean macro, AnnotatedClass k, Object o, Method m, int param_set )
	throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		if ( macro ) {
			markMacro( k, o, m, param_set );
		} else {
			markMicro( k, o, m, param_set );
		}
	}

	private void bench()
	throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{

		for( ClassAndInstance cai: alcai ) {

			AnnotatedClass k = cai.klass;
			Object o = cai.instance;

			for( int row=0; row < param_values.length; row++ ) {

				// set all parameters for a specific set of trials
				for( int col=0; col < param_values[ row ].length; col++ ) {
					Field f = param_fields[ col ];
					f.set( o, param_values[ row ][ col ].value );
				}

				try {

					// allow the benchmarking class to perform some misc tasks before executing a set of trials
					for( Method m: k.getBeforeExperimentMethods() ) {
						m.invoke( o );
					}

					// perform micro benchmarking (slightly more complicated than macrobenchmarking)
					for( Method m: k.getBenchmarkMethods() ) {
						mark( false, k, o, m, row );
					}

					// perform macro benchmarking
					for( Method m: k.getMacrobenchmarkMethods() ) {
						mark( true, k, o, m, row );
					}

					// allow the benchmarking class to perform some misc tasks before executing a set of trials
					for( Method m: k.getAfterExperimentMethods() ) {
						m.invoke( o );
					}

				} catch( SkipThisScenarioException e ) {
					continue;
				}

			}
		}
	}

/*############################################################################
 *                         Single Point of Entry
 *############################################################################*/

	public static void evaluate( Arguments a, Context c )
	throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException
	{
		if ( ! a.bench_classes.isEmpty() ) {
			Algorithm algo = new Algorithm( a, c );
			algo.setup();
			algo.warmUp();
			algo.bench();
		}
	}
}
