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

public class Algorithm {

	// We make the assumption (yes, I know) that our sampled random variable (execution time)
	// will have a (non-standard) normal distribution. This is later validated using the
	// Chi-Squared test.

	// we want our accepted values for sample mean and sample variance to be within
	// Q_ACCEPTANCE standard deviations from the mean.
	static final double Q_ACCEPTANCE = 1 / 5D;

	// we want our accepted values for sample mean and sample variance to have a
	// confidence value of P_CONFIDENCE - i.e. there is ( 1 - P_CONFIDENCE ) chance for error.
	static final double P_CONFIDENCE = 0.95;

	static final int MIN_TRIALS;
	static {
		MIN_TRIALS =
			Math.max(
				OnlineStatistics.MIN_N_BEFORE_VALID_VARIANCE,
				ChiSquared.minSamples( Q_ACCEPTANCE, P_CONFIDENCE )
			);
	}
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
		MAX_TRIALS = MIN_TRIALS * arguments.max_trials;
	}

	private static class ClassAndInstance {
		public final SlideRuleAnnotations klass;
		public final Object instance;
		public ClassAndInstance( SlideRuleAnnotations klass, Object instance ) {
			this.klass = klass;
			this.instance = instance;
		}
		@Override
		public String toString() {
			return klass.toString();
		}
	}

	private static int verbose = 0;
	private static void Dn( int n, Object o ) {
		if ( n <= verbose ) {
			System.err.println( "DEBUG: " + o );
		}
	}
	private static void D( Object o ) {
		Dn( 0, o );
	}
	private static void D1( Object o ) {
		Dn( 1, o );
	}
	private static void D2( Object o ) {
		Dn( 2, o );
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
	public static PolymorphicType[][] permute( Field[] param_fields )
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
		PolymorphicType[][] param_values = new PolymorphicType[ nrows ][ ncols ];

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
		return param_values;
	}

	private void permute()
	throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		param_values = permute( param_fields );
	}

	private void setup()
	throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		for( SlideRuleAnnotations ac: context.getAnnotatedClasses() ) {
			Class<?> an = ac.getAnnotatedClass();
			Object o = an.newInstance();
			ClassAndInstance cai = new ClassAndInstance( ac, o );
			alcai.add( cai );
		}

		SlideRuleAnnotations proto;
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
	private static final int MAX_REPS_FOR_STABLE_REPS = 100000000;

	/**
	 * Find the index of the first root of {@code f}.
	 * @param f a series of samples
	 * @param n the number of valid samples in f
	 * @return the index of the first root, or -1 if no root was found
	 * @see <a href="http://en.wikipedia.org/wiki/Newton's_method">The Newton-Raphson Method</a>
	 */
	private static int newtonRaphson( double[] f, int n ) {
		int r = -1;

		double[] diff = new double[ n - 1 ];
		for( int i = 0; i < diff.length; i++ ) {
			if ( 0 == i || diff.length - 1 == i ) {
				diff[ i ] = f[ i + 1 ] - f[ i ];
			} else {
				diff[ i ] = ( f[ i + 1 ] - f[ i - 1 ] ) / 2;
			}
		}

		D2( "1st derivative of f is " + Arrays.toString( diff ) );

		for( int i = 0; i < diff.length; i++ ) {
			if ( 0 == diff[ i ] ) {
				r = i;
				D2( "found root at index " + i );
				break;
			}
		}
		return r;
	}

	/**
	 * Find the index of the first minimum of {@code f}.
	 * @param f a series of samples
	 * @param n the number of valid samples in f
	 * @return the index of the first minimum, or -1 if no minimum was found
	 * @see <a href="http://en.wikipedia.org/wiki/Newton%27s_method_in_optimization">Newton's Method in Optimization</a>
	 */
	private static int newton( double[] f, int n ) {
		int r = -1;
		// equivalent to 3-point centered-difference, but does not require calculating difference
		if ( -1 == r ) {
			for( int i = 0; i < n; i++ ) {
				if ( i > 0 && i < n - 1 ) {
					if ( f[ i ] > f[ i ] && f[ i + 1 ] > f[ i ] ) {
						D2( "found minimum at index " + i );
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

		D2( "choosing number of reps to use for method " + m );

		Object dummy, dummy2;

		// populate reps with orders of magnitude
		for( int i = 0, j = 1; i < reps.length; reps[ i ] = j, i++, j *= 10 );

		D( "warming up method " + m + " with " + N_WARMUP_REPS + " calls" );
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

			D2( "averaged_elapsed_ns[] is " + Arrays.toString( Arrays.copyOf( average_elapsed_ns, i+1 ) ) );

			D2( "trying Newton-Raphson" );
			int r2 = newtonRaphson( average_elapsed_ns, i+1 );
			if ( -1 != r2 ) {
				r = reps[ r2 ];
				break;
			}

			D2( "trying Newton" );
			r2 = newton( average_elapsed_ns, i+1 );
			if ( -1 != r2 ) {
				r = reps[ r2 ];
				break;
			}

			if ( elapsed_ns >= SAFE_NS_FOR_STABLE_REPS ) {
				D( "stability time limit exceeded");
				break;
			}
		}

		if ( -1 == r ) {
			r = reps[ reps.length - 1 ];
		}

		D1( "chose " + r + " reps" );

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
			D2( "will not validate statistical model with less than MIN_TRIALS ( " + MIN_TRIALS +" ) samples"  );
			return false;
		}

		D1( "checking statistical model based on " + trials.size() + " samples" );

		double[] observed = new double[ trials.size() ];

		int i=0;
		for( Trial t: trials ) {
			boolean found_elapsed_time_ns = false;
			for( Measurement measure: t.measurements() ) {
				if ( ( ! found_elapsed_time_ns ) && "elapsed_time_ns".equals( measure.description() ) ) {
					observed[ i ] = (double)(Double) measure.value().value;
					found_elapsed_time_ns = true;
					break;
				}
			}
			if ( ! found_elapsed_time_ns ) {
				throw new IllegalStateException();
			}
			i++;
		}

		OfflineStatistics observed_stats = new OfflineStatistics( observed );
		// XXX: not a guarantee that "optimal" histogram size will be long enough to calculate variance
		int sz = Histogram.partition( observed_stats );
		if ( sz <= AStatistics.MIN_N_BEFORE_VALID_VARIANCE ) {
			D2( "'optimal' histogram size was only " + sz + ". padding out to " + (AStatistics.MIN_N_BEFORE_VALID_VARIANCE + 1) );
			sz = AStatistics.MIN_N_BEFORE_VALID_VARIANCE + 1;
		}
		Histogram observed_hist = new Histogram( sz, observed_stats );
		Histogram normal_hist = Normal.histogram( observed_hist.size(), observed_stats.mean(), observed_stats.standardDeviation() );

		boolean r = ChiSquared.test( P_CONFIDENCE, normal_hist, observed_hist );

		if ( r ) {
			D( "statistical model validated: " + observed_stats );
		}
		return r;
	}


	private void mark( boolean macro, SlideRuleAnnotations ann, Object o, Method m, int param_set )
	throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		Object dummy = null;
		final boolean dry_run = arguments.dry_run;
		long trial_start_ns = 0;
		long trial_end_ns = 0;
		long elapsed_ns = 0;
		long trial_start_ms = 0;
		long trial_stop_ms = 0;
		double average_elapsed_time;
		boolean students_t_test_passed = false;
		boolean validated_statistical_model = false;

		ArrayList<Trial> trials = new ArrayList<Trial>();
		OnlineStatistics ts = new OnlineStatistics();

		int reps = macro ? 1 : chooseReps( o, m );

		UUID id = UUID.randomUUID();

		D( "entering trials loop at " + System.currentTimeMillis() );

		D( "MIN_TRIALS for " + P_CONFIDENCE + " confidence and " + Q_ACCEPTANCE + "std.dev acceptance is " + MIN_TRIALS );
		D( "MAX_TRIALS is " + MAX_TRIALS );

		// proceed until the result of the trials is statistically significant
		for( ; ! validated_statistical_model && trials.size() < MAX_TRIALS; ) {

			if ( macro ) {
				for( Method b4: ann.getBeforeRepMethods() ) {
					b4.invoke( o );
				}
			}

			SimpleTrial st = new SimpleTrial( id, ann, m, param_fields, param_values[ param_set ] );
			ts.clear();

			trial_start_ms = System.currentTimeMillis();
			D2( "starting trial at " + trial_start_ms );
			trial_stop_ms = arguments.time_limit >= 0 ? ( trial_start_ms + 1000 * arguments.time_limit ): 0;
			if ( 0 != trial_stop_ms ) {
				D2( "trial must finish by " + trial_stop_ms );
			}

			D2( "entering reps loop" );
			for( ;; ) {

				trial_start_ns = System.nanoTime();

				if ( ! dry_run ) {
					if ( macro ) {
						dummy = m.invoke( o );
					} else {
						dummy = m.invoke( o, reps );
					}
				}

				trial_end_ns = System.nanoTime();

				elapsed_ns = elapsed( trial_start_ns, trial_end_ns );
				average_elapsed_time = elapsed_ns / reps;
				ts.update( average_elapsed_time );

				if ( 0 != trial_stop_ms && System.currentTimeMillis() >= trial_stop_ms ) {
					D( "breaking out of reps loop because time limit was exceeded"  );
					break;
				}

				if ( macro ) {
					D2( "breaking out of reps loop because method is macrobenchmark " );
					break;
				} else {
					if ( ! students_t_test_passed ) {
						if ( ts.size() > 2 * AStatistics.MIN_N_BEFORE_VALID_VARIANCE ) {
							students_t_test_passed = StudentsT.test( ts.size(), P_CONFIDENCE, ts.mean(), ts.standardDeviation() );
							if ( !students_t_test_passed ) {
								D2("failed Student's t-test");
								ts.clear();
							}
						}
					}
					if ( students_t_test_passed ) {
						D2("breaking out of reps loop because measurements passed Student's t-test");
						break;
					}
				}
			}

			D2( "exited reps loop" );

			prepareMeasurements( st, reps, trial_start_ms, trial_start_ns, trial_end_ns, ts, dummy );
			context.results_processor.processTrial( st );
			trials.add( st );
			validated_statistical_model = validateStatisticalModel( trials );

			if ( macro ) {
				for( Method aft: ann.getAfterRepMethods() ) {
					aft.invoke( o );
				}
			}
		}

		D( "exited trials loop at " + System.currentTimeMillis() );

		byte[] line = null;
		if ( arguments.debug >= 0 ) {
			line = new byte[ 70 ];
			Arrays.fill( line, (byte)'=' );
			D( new String( line ) );
		}

		if ( trials.size() >= MAX_TRIALS ) {
			D( "failed to validate statistical model for " + trials.get( 0 ) + " after " + trials.size() + " trials");
			SimpleTrial warning_trial = new SimpleTrial( id, ann, m, param_fields, param_values[ param_set ] );
			SimpleMeasurement warning_measurement = new SimpleMeasurement( "warning", new PolymorphicType( String.class, new String( "failed to validate statistical model" ) ) );
			warning_trial.addMeasurement( warning_measurement );
			context.results_processor.processTrial( warning_trial );
		}
	}

	private void bench()
	throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException
	{

		for( ClassAndInstance cai: alcai ) {

			SlideRuleAnnotations k = cai.klass;
			Object o = cai.instance;

			// was complaining about "java.lang.IllegalArgumentException: Can not set int field examples.FactorialBenchmark.number to examples.SumBenchmark"
			ArrayList<Field> alf = new ArrayList<Field>();
			alf.addAll( k.getParamFields() );
			param_fields = alf.toArray( new Field[0] );

			for( int row=0; row < param_values.length; row++ ) {

				// set all parameters for a specific set of trials
				for( int col=0; col < param_values[ row ].length; col++ ) {
					Field f = param_fields[ col ];
					f.setAccessible( true );
					f.set( o, param_values[ row ][ col ].value );
				}

				D( "benchmarking " + cai.klass.getAnnotatedClass().getName() + " with parameters " + PolymorphicType.nameParams( param_fields, param_values[row] ) );

				try {

					// allow the benchmarking class to perform some misc tasks before executing a set of trials
					for( Method m: k.getBeforeExperimentMethods() ) {
						m.invoke( o );
					}

					// perform micro benchmarking (slightly more complicated than macrobenchmarking)
					for( Method m: k.getBenchmarkMethods() ) {
						D( "Microbenchmarking " + cai.klass.getAnnotatedClass().getName() + "." + m.getName() + "()" );
						mark( false, k, o, m, row );
					}

					// perform macro benchmarking
					for( Method m: k.getMacrobenchmarkMethods() ) {
						D( "Macrobenchmarking " + cai.klass.getAnnotatedClass().getName() + "." + m.getName() + "()" );
						mark( true, k, o, m, row );
					}

					// allow the benchmarking class to perform some misc tasks before executing a set of trials
					for( Method m: k.getAfterExperimentMethods() ) {
						m.invoke( o );
					}
				} catch( SkipThisScenarioException e ) {
					continue;
				} catch( InvocationTargetException e ) {
					Throwable t = e.getCause();
					if ( t instanceof SkipThisScenarioException ) {
						continue;
					}
					throw e;
				}
			}
			D( "finished all parameter permutations. moving on to next class" );
		}
		context.results_processor.close();
	}

/*############################################################################
 *                         Single Point of Entry
 *############################################################################*/

	public static void evaluate( Arguments a, Context c )
	throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException, IOException
	{
		verbose = a.debug;

		if ( ! c.getAnnotatedClasses().isEmpty() ) {
			Algorithm algo = new Algorithm( a, c );
			algo.setup();
			algo.bench();
		}
	}
}
