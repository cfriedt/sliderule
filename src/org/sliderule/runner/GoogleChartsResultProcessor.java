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
import java.nio.file.*;
import java.text.*;
import java.util.*;

import org.sliderule.model.*;
import org.sliderule.stats.*;

/**
 * <p><b>Google Charts Result Processor</b></p>
 *
 * <p>This class plots the results of benchmarking trials using
 * <a href="https://developers.google.com/chart/">Google Charts API</a>.
 * SlideRule benchmark results are written to
 * <a href="http://en.wikipedia.org/wiki/HTML">HTML</a>
 * files and are grouped according to the following guidelines.
 *
 * <ol>
 *   <li>
 *       One set of graphs is generated per set of parameters.
 *   </li>
 *   <li>
 *       Several graphs may appear in one output file, if they are part of the same
 *       set (i.e. if they are generated with the same set of parameters).
 *   </li>
 *   <li>
 *       Microbenchmarks will only appear on the same graph with other microbenchmarks.
 *   </li>
 *   <li>
 *       Macrobenchmarks will only appear on the same graph with other macrobenchmarks.
 *   </li>
 *   <li>
 *       If there is only one class under test, all microbenchmarks for that class
 *       will appear on the same graph. Similarly, all macrobenchmarks for that class
 *       will appear on a separate graph.
 *   </li>
 *   <li>
 *       If there are multiple classes under test, all identically-named microbenchmarks
 *       selected from each class will be grouped on one graph. Similarly, all
 *       identically-named macrobenchmarks selected from each class will appear
 *       on a separate graph.
 *   </li>
 *   <li>
 *       If parametric graphs are generated (see below), the swept parameter is
 *       removed from the set of parameters (thereby generating a smaller set of parameters).
 *       The remaining parameters in the set are pinned at their base values. Only one variable
 *       is varied at a time per parametric sweep.
 *   </li>
 * </ol>
 * </p>
 *
 * <p><b>System Properties</b></p>
 *
 * <p>By setting the config property
 * <b>-Corg.sliderule.runner.GoogleChartsResultProcessor.plot.histogram=true</b>,
 * this class will not only output graphs for microbenchmarks and macrobenchmarks, but it will also
 * plot the histogram of the collected data with direct comparison to the {@link Normal} distribution
 * it was validated against.
 * </p>
 * <p>By setting the config property
 * <b>-Corg.sliderule.runner.GoogleChartsResultProcessor.output.directory=path/to/somewhere</b>,
 * this class will generate output files in the named directory instead of the current working directory.
 * </p>
 *
 * @author <a href="mailto:chrisfriedt@gmail.com">Christopher Friedt</a>
 * @see <a href="https://developers.google.com/chart/">Google Charts API</a>
 */
public class GoogleChartsResultProcessor extends InMemoryResultProcessor {

	private static final String output_directory_property = GoogleChartsResultProcessor.class.getName() + ".output.directory";
	static final String output_directory;

	private static final String plot_histogram_property = GoogleChartsResultProcessor.class.getName() + ".plot.histogram";
	static final boolean plot_histogram;

	private static final String parametric_sweep_property = GoogleChartsResultProcessor.class.getName() + ".plot.parametric.sweep";
	static final boolean parametric_sweep;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		main();
	}

	private void main()
	throws IOException
	{
		if ( trial_set.isEmpty() ) {
			return;
		}

		Trial t = trial_set.firstEntry().getValue().get( 0 );
		if ( ! ( t instanceof SimpleTrial ) ) {
			return;
		}

		SimpleTrial st = (SimpleTrial) t;

		SortedSet<Field> field_set = st.getSlideRuleAnnotations().getParamFields();
		if ( field_set.isEmpty() ) {
			return;
		}

		PolymorphicType[][] pmt = null;
		Field[] field_array = field_set.toArray( new Field[ 0 ] );
		List<Field> fields = Arrays.asList( field_array );
		try {
			pmt = Algorithm.permute( field_array );
		} catch ( InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
			return;
		}
		if ( null == pmt |  pmt.length < 1 ) {
			return;
		}

		// create one file containing up to two graphs for each permutation of parameters (one for microbenchmarks, one for macrobenchmarks)
		for( int row = 0; row < pmt.length; row++ ) {

			List<PolymorphicType> lpmt = Arrays.asList( pmt[ row ] );
			TreeMap<UUID, ArrayList<Trial>> subset = filterByParamValue( trial_set, fields, lpmt );

			if ( ! subset.isEmpty() ) {
				File file = genFile( subset );
				GoogleChartsWriter gcw = new GoogleChartsWriter( date, new FileOutputStream( file ), subset );
				gcw.write();
				gcw.close();
			}
		}

		if ( parametric_sweep ) {
			// create one file containing up to two graphs (one for microbenchmarks, one for macrobenchmarks) for each parameter
			// but only if there is more then one value for that parameter
			for( int col = 0; col < field_array.length; col++ ) {

				TreeSet<PolymorphicType> unique_values = new TreeSet<PolymorphicType>();

				for( int row = 0; row < pmt.length; row++ ) {
					unique_values.add( pmt[ row ][ col ] );
				}

				if ( unique_values.size() > 1 ) {

					PolymorphicType[] base_case = new PolymorphicType[ pmt[ 0 ].length ];
					System.arraycopy( pmt[ 0 ], 0, base_case, 0, base_case.length );

					TreeMap<UUID, ArrayList<Trial>> subset = new TreeMap<UUID, ArrayList<Trial>>();

					for( PolymorphicType v: unique_values ) {
						base_case[ col ] = v;
						List<PolymorphicType> values = Arrays.asList( base_case );
						TreeMap<UUID, ArrayList<Trial>> q = filterByParamValue( trial_set, fields, values );
						subset.putAll( q );
					}

					File file = genFile( subset );
					GoogleChartsWriter gcw = new GoogleChartsWriter( date, new FileOutputStream( file ), subset );
					gcw.write();
					gcw.close();
				}
			}
		}
	}

	private String genFileName( String tag ) {
		final String sliderule = "sliderule";
		final String sep = "-";
		final String dothtml = ".html";
		String r = output_directory + File.separator + sliderule + sep + date + sep + epoch + sep + tag + dothtml;
		return r;
	}

	static boolean trialsAreParametricSweep( TreeMap<UUID,ArrayList<Trial>> trials ) {
		return -1 != parametricSweepIndex( trials );
	}

	@SuppressWarnings("unchecked")
	static int parametricSweepIndex( TreeMap<UUID, ArrayList<Trial>> trials ) {

		// for this to be the case, only one parameter may be changed throughout all of the
		// trials recorded for an experiment

		int r = -1;

		Trial t;
		SimpleTrial st;
		PolymorphicType[] pmt;

		t = trials.firstEntry().getValue().get( 0 );
		if ( ! ( t instanceof SimpleTrial ) ) {
			throw new IllegalStateException();
		}
		st = (SimpleTrial) t;

		HashSet<PolymorphicType>[] fields_varied = new HashSet[ st.getParamValue().length ];
		for( int i = 0; i < fields_varied.length; i++ ) {
			fields_varied[ i ] = new HashSet<PolymorphicType>();
		}

		for( ArrayList<Trial> alt: trials.values() ) {
			t = alt.get( 0 );
			if ( ! ( t instanceof SimpleTrial ) ) {
				throw new IllegalStateException();
			}
			st = (SimpleTrial) t;
			pmt = st.getParamValue();
			for( int i = 0; i < pmt.length; i++ ) {
				fields_varied[ i ].add( pmt[ i ] );
			}
		}

		int idx = -1;
		int n_fields_with_more_than_one_value = 0;
		for( int i = 0; i < fields_varied.length; i++ ) {
			if ( fields_varied[ i ].size() > 1 && -1 == idx ) {
				idx = i;
			}
			n_fields_with_more_than_one_value += fields_varied[ i ].size() > 1 ? 1 : 0;
		}

		if ( 1 == n_fields_with_more_than_one_value ) {
			r = idx;
		}

		return r;
	}

	@SuppressWarnings("unchecked")
	static <T extends Object> T[] extractOneFromArray( T[] array, int index_to_extract ) {
		T[] r = (T[]) Array.newInstance( array[ 0 ].getClass(), array.length - 1 );
		for( int i = 0, j = 0; i < array.length; i++, j += ( j == index_to_extract ) ? 0 : 1 ) {
			if ( !( i == index_to_extract ) ) {
				r[ j ] = array[ i ];
			}
		}
		return r;
	}

	private File genFile( TreeMap<UUID, ArrayList<Trial>> trials ) throws IOException {

		// Parameters will either all be the same or not.
		// If they are not the same, then we're performing
		// a parametric sweep and should tag it "sweep-<field>".
		// If all of the parameters are the same then we should
		// call tag it "<param:val><param:val>..."

		SimpleTrial st;

		Trial t = trials.firstEntry().getValue().get( 0 );
		if ( ! ( t instanceof SimpleTrial ) ) {
			throw new IllegalStateException();
		}
		st = (SimpleTrial) t;

		Field[] field = st.getParam();

		boolean sweep = trialsAreParametricSweep( trials );
		String tag = "";

		if ( sweep ) {
			int delta = parametricSweepIndex( trials );
			if ( -1 == delta ) {
				throw new IllegalStateException();
			}
			tag = "sweep:" + field[ delta ].getName();
		} else {
			tag = PolymorphicType.nameParams( field, st.getParamValue() );
			tag = tag.replace( "[", "" );
			tag = tag.replace( "]", "" );
		}

		String file_name = genFileName( tag );
		File file = new File( file_name );
		if ( file.isDirectory() ) {
			throw new IOException( "file '" + file_name + "' exists and is a directory." );
		}
		file.mkdirs();
		file.delete();

		return file;
	}

	final String date = new SimpleDateFormat( "yyyyMMdd" ).format( new Date() );
	final long epoch = System.currentTimeMillis();

	static {
		String s;
		boolean b;
		// XXX: arguments.static_config_properties. does not seem to work
		// arguments.static_config_properties.
		s = System.getProperty( output_directory_property );
		output_directory = null == s ? Paths.get( "" ).toAbsolutePath().toString() : s;
		s = System.getProperty( plot_histogram_property );
		b = Boolean.parseBoolean( s );
		plot_histogram = b;
		s = System.getProperty( parametric_sweep_property );
		parametric_sweep = true;
	}
}
