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
import org.sliderule.stats.*;

class Algorithm {

	// We make the assumption (yes, I know) that our sampled random variable (execution time)
	// will have a (non-standard) normal distribution. This is later validated using the
	// Chi-Squared test.

	// we want our accepted values for sample mean and sample variance to be within
	// Q_ACCEPTANCE standard deviations from the mean.
	static final double Q_ACCEPTANCE = 1 / 5D;

	// we want our accepted values for sample mean and sample variance to have a
	// confidence value of P_CONFIDENCE - i.e. there is ( 1 - P_CONFIDENCE ) chance for error.
	static final double P_CONFIDENCE = 0.95;

	final int MIN_TRIALS;

	final Arguments arguments;
	final Context context;
	final ArrayList<ClassAndInstance> alcai;
	Field[] param_fields;
	PolymorphicType[][] param_values;


	private Algorithm( Arguments arguments, Context context ) {
		this.arguments = arguments;
		this.context = context;
		this.alcai = new ArrayList<ClassAndInstance>();
		MIN_TRIALS =
			Math.max(
				Math.max(
					arguments.trials,
					ChiSquared.minSamples( Q_ACCEPTANCE, P_CONFIDENCE )
				),
				OnlineStatistics.MIN_N_BEFORE_VALID_VARIANCE
			);
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
	static int euprod( int[] cardinality, int hold_index ) {
		int result = 1;
		for( int i=0; i < cardinality.length; result *= ( ( -1 == hold_index || i > hold_index ) ? cardinality[ i ] : 1 ), i++ );
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

		// generate the Euclidean parameter space
		permute();
	}

/*############################################################################
 *      Warm-up the JVM / JIT (trigger tier1/2 compiler, inlining, ... )
 *############################################################################*/

	private static final int N_WARMUP_REPS;

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
		final int Mi = (1 << 20);
		long trial_start_ns;
		long trial_end_ns;
		long elapsed_ns;
		long trial_start_ms;

		// reuse this one over and over
		OnlineStatistics ts = new OnlineStatistics();

		ArrayList<Trial> trials = new ArrayList<Trial>();

		// warm-up
		m.invoke( o, N_WARMUP_REPS );

		// proceed until the result of the trials is statistically significant
		for( int trial=0; trial < MIN_TRIALS; trial++ ) {

			SimpleTrial st = new SimpleTrial( k.getAnnotatedClass(), m, param_fields, param_values[ param_set ] );

			for( int reps = 1; reps < 10 * Mi; reps <<= 1 ) {

				for( Method b4: k.getBeforeExperimentMethods() ) {
					b4.invoke( o );
				}

				trial_start_ms = System.currentTimeMillis();
				trial_start_ns = System.nanoTime();

				m.invoke( o, reps );

				trial_end_ns = System.nanoTime();

				for( Method aft: k.getAfterExperimentMethods() ) {
					aft.invoke( o );
				}

				elapsed_ns = elapsed( trial_start_ns, trial_end_ns );
				ts.update( elapsed_ns );

				SimpleMeasurement mean_ns_measurement = new SimpleMeasurement( "mean_ns", new PolymorphicType( double.class, ts.getMean() ) );
				SimpleMeasurement variance_ns_measurement = new SimpleMeasurement( "variance_ns", new PolymorphicType( double.class, ts.getVariance() ) );

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
		// should validate statistical model here
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
			algo.bench();
		}
	}
}
