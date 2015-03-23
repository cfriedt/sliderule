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

import java.io.*;
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
	final int MAX_TRIALS;

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
		MAX_TRIALS = 30 * MIN_TRIALS;
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

	private static final long SAFE_NS_FOR_STABLE_REPS = 10000000000L;
	private static final int MAX_REPS_FOR_STABLE_REPS = 1000000000;

	private int quickChooseReps( int[] reps, double[] average_elapsed_ns, int n ) {
		int r = -1;
		double[] diff = new double[ n - 1 ];
		for( int i = 0; i < diff.length; i++ ) {
			if ( 0 == i || diff.length - 1 == i ) {
				diff[ i ] = average_elapsed_ns[ i+1 ] - average_elapsed_ns[ i ];
			} else {
				diff[ i ] = ( average_elapsed_ns[ i+1 ] - average_elapsed_ns[ i - 1 ] ) / 2;
			}
		}

		for( int i = 0; i < diff.length; i++ ) {
			if ( 0 == diff[ i ] ) {
				r = reps[ i ];
				break;
			}
		}

		if ( -1 == r ) {
			for( int i = 0; i < n; i++ ) {
				if ( i > 0 && i < n - 1 ) {
					if ( average_elapsed_ns[ i ] > average_elapsed_ns[ i ] && average_elapsed_ns[ i + 1 ] > average_elapsed_ns[ i ] ) {
						r = i;
					}
				}
			}
		}
		return r;
	}
	private int chooseReps( Object o, Method m )
	throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		int r = -1;

		int[] reps = new int[ (int) Math.log10( MAX_REPS_FOR_STABLE_REPS ) ];
		double[] average_elapsed_ns = new double[ reps.length ];
		long start_ns;
		long end_ns;
		long elapsed_ns;

		Object dummy, dummy2;

		// populate reps with orders of magnitude
		for( int i = 0, j = 1; i < reps.length; reps[ i ] = j, i++, j *= 10 );

		// warm-up
		dummy = m.invoke( o, N_WARMUP_REPS );

		for( int i = 0; i < reps.length; i++ ) {

			start_ns = System.nanoTime();
			dummy2 = m.invoke( o, reps[ i ] );
			end_ns = System.nanoTime();
			elapsed_ns = elapsed( start_ns, end_ns );
			average_elapsed_ns[ i ] = elapsed_ns / reps[ i ];

			if ( null == dummy ) {
				dummy = dummy2;
			}

			int r2 = quickChooseReps( reps, average_elapsed_ns, i+1 );
			if ( -1 != r2 ) {
				r = r2;
				break;
			}

			if ( elapsed_ns >= SAFE_NS_FOR_STABLE_REPS ) {
				break;
			}
		}

		if ( -1 == r ) {
			r = reps[ reps.length - 1 ];
		}

		return r;
	}

	private void prepareMeasurements( SimpleTrial st, int reps, long trial_start_ms, long trial_start_ns, long trial_end_ns, OnlineStatistics ts, Object dummy ) {

		SimpleMeasurement mean_ns_measurement = new SimpleMeasurement( "elapsed_time_ns", new PolymorphicType( double.class, ts.mean() ) );
		SimpleMeasurement variance_ns_measurement = new SimpleMeasurement( "variance_ns", new PolymorphicType( double.class, ts.variance() ) );
		SimpleMeasurement rep_measurement = new SimpleMeasurement( "reps", new PolymorphicType( int.class, reps ) );
		SimpleMeasurement trial_start_ms_measurement = new SimpleMeasurement( "trial_start_ms", new PolymorphicType( long.class, trial_start_ms ) );
		SimpleMeasurement trial_start_ns_measurement = new SimpleMeasurement( "trial_start_ns", new PolymorphicType( long.class, trial_start_ns ) );
		SimpleMeasurement trial_end_ns_measurement = new SimpleMeasurement( "trial_end_ns", new PolymorphicType( long.class, trial_end_ns ) );
		SimpleMeasurement dummy_measurement;

		if ( null == dummy ) {
			dummy_measurement = new SimpleMeasurement( "dummy", new PolymorphicType( String.class, "the dummy was null" ) );
		} else {
			dummy_measurement = new SimpleMeasurement( "dummy", new PolymorphicType( dummy.getClass(), "" + dummy ) );
		}

		st.addMeasurement( mean_ns_measurement );
		st.addMeasurement( variance_ns_measurement );
		st.addMeasurement( rep_measurement );
		st.addMeasurement( trial_start_ms_measurement );
		st.addMeasurement( trial_start_ns_measurement );
		st.addMeasurement( trial_end_ns_measurement );
		st.addMeasurement( dummy_measurement );
	}

	private boolean validateStatisticalModel( ArrayList<Trial> trials ) {

		if ( trials.size() < MIN_TRIALS ) {
			return false;
		}

		double[] elapsed_time_ns = new double[ trials.size() ];

		int i=0;
		for( Trial t: trials ) {
			boolean found_elapsed_time_ns = false;
			for( Measurement measure: t.measurements() ) {
				if ( ( ! found_elapsed_time_ns ) && "elapsed_time_ns".equals( measure.description() ) ) {
					elapsed_time_ns[ i ] = (double)(Double) measure.value().value;
					found_elapsed_time_ns = true;
					break;
				}
			}
			if ( ! found_elapsed_time_ns ) {
				throw new IllegalStateException();
			}
			i++;
		}

		OfflineStatistics elapsed_time_ns_stats = new OfflineStatistics( elapsed_time_ns );
		Histogram elapsed_time_ns_hist = new Histogram( elapsed_time_ns_stats );

		OfflineStatistics normal_stats = new OfflineStatistics( Normal.pdf( elapsed_time_ns_stats.size(), elapsed_time_ns_stats.mean(), elapsed_time_ns_stats.standardDeviation() ) );
		Histogram normal_hist = new Histogram( elapsed_time_ns_hist.size(), normal_stats );
		final boolean normalize = true;
		boolean elapsed_time_ns_is_normally_distributed = ChiSquared.test( P_CONFIDENCE, normalize, normal_hist, elapsed_time_ns_hist );

		boolean r = elapsed_time_ns_is_normally_distributed;
		return r;
	}


	private void mark( boolean macro, AnnotatedClass k, Object o, Method m, int param_set )
	throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		Object dummy;
		long trial_start_ns = 0;
		long trial_end_ns = 0;
		long elapsed_ns = 0;
		long trial_start_ms = 0;
		double average_elapsed_time;
		boolean students_t_test_passed = false;
		boolean validated_statistical_model = false;

		ArrayList<Trial> trials = new ArrayList<Trial>();
		OnlineStatistics ts = new OnlineStatistics();

		for( Method b4: k.getBeforeExperimentMethods() ) {
			b4.invoke( o );
		}

		int reps = macro ? 1 : chooseReps( o, m );
		int trial;

		UUID id = UUID.randomUUID();

		// proceed until the result of the trials is statistically significant
		for( trial=0; ! validated_statistical_model && trial < MAX_TRIALS; trial++ ) {

			SimpleTrial st = new SimpleTrial( id, k.getAnnotatedClass(), m, param_fields, param_values[ param_set ] );
			ts.clear();

			for( ;; ) {

				trial_start_ms = System.currentTimeMillis();
				trial_start_ns = System.nanoTime();

				if ( macro ) {
					dummy = m.invoke( o );
				} else {
					dummy = m.invoke( o, reps );
				}

				trial_end_ns = System.nanoTime();

				elapsed_ns = elapsed( trial_start_ns, trial_end_ns );
				average_elapsed_time = elapsed_ns / reps;
				ts.update( average_elapsed_time );

				if ( macro ) {
					break;
				} else {
					if ( ! students_t_test_passed ) {
						if ( ts.size() > 2 * AStatistics.MIN_N_BEFORE_VALID_VARIANCE ) {
							students_t_test_passed = StudentsT.test( ts.size(), P_CONFIDENCE, ts.mean(), ts.standardDeviation() );
							if ( !students_t_test_passed ) {
								ts.clear();
							}
						}
					}
					if ( students_t_test_passed ) {
						break;
					}
				}
			}

			prepareMeasurements( st, reps, trial_start_ms, trial_start_ns, trial_end_ns, ts, dummy );
			context.results_processor.processTrial( st );
			trials.add( st );

			validated_statistical_model = validateStatisticalModel( trials );
		}

		if ( trial >= MAX_TRIALS ) {
			SimpleTrial warning_trial = new SimpleTrial( id, k.getAnnotatedClass(), m, param_fields, param_values[ param_set ] );
			SimpleMeasurement warning_measurement = new SimpleMeasurement( "warning", new PolymorphicType( String.class, new String( "failed to validate statistical model" ) ) );
			warning_trial.addMeasurement( warning_measurement );
			context.results_processor.processTrial( warning_trial );
		}

		for( Method aft: k.getAfterExperimentMethods() ) {
			aft.invoke( o );
		}
	}

	private void bench()
	throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException
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
		context.results_processor.close();
	}

/*############################################################################
 *                         Single Point of Entry
 *############################################################################*/

	public static void evaluate( Arguments a, Context c )
	throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException, IOException
	{
		if ( ! a.bench_classes.isEmpty() ) {
			Algorithm algo = new Algorithm( a, c );
			algo.setup();
			algo.bench();
		}
	}
}
