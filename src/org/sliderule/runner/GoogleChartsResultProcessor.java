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
import java.text.*;
import java.util.*;

import org.sliderule.model.*;
import org.sliderule.stats.*;

/**
 * <p><b>Google Charts Result Processor</b></p>
 *
 * <p>This class plots the results of benchmarking trials using
 * <a href="https://developers.google.com/chart/">Google Charts API</a>.
 * SlideRule benchmark results are grouped parametrically according to the following
 * guidelines.
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
 * </ol>
 * </p>
 *
 * <p><b>System Properties</b></p>
 *
 * <p>By setting the config property
 * <b>-Corg.sliderule.runner.GoogleChartsResultProcessor.plot.histogram=true</b>,
 * this class will not only output graphs for microbenchmarks and macrobenchmarks, but it will also
 * plot the histogram of the collected data with direct comparison to the {@link Normal} distribution
 * it was validated against to.
 * </p>
 * <p>By setting the config property
 * <b>-Corg.sliderule.runner.GoogleChartsResultProcessor.output.directory=path/to/somewhere</b>,
 * this class will generate output file in the named directory instead of the current working directory.
 * </p>
 *
 * @author <a href="mailto:chrisfriedt@gmail.com">Christopher Friedt</a>
 * @see <a href="https://developers.google.com/chart/">Google Charts API</a>
 */
public class GoogleChartsResultProcessor extends InMemoryResultProcessor {

	// TODO: document output_directory system property
	private static final String output_directory_property = GoogleChartsResultProcessor.class.getName() + ".output.directory";
	static final String output_directory;

	// TODO: document plot_histogram system property
	private static final String plot_histogram_property = GoogleChartsResultProcessor.class.getName() + ".plot.histogram";
	static final boolean plot_histogram;

	static class Crunched {
		Trial proto;
		IStatistics is;
		AnnotatedClass clazz;
		Crunched( Trial proto, IStatistics is ) {
			this.proto = proto;
			this.is = is;
			try {
				clazz = new AnnotatedClass( ClassLoader.getSystemClassLoader().loadClass( getClassName() ) );
			} catch ( ClassNotFoundException e ) {
				throw new IllegalStateException();
			}
		}
		boolean isMicro() {
			String n = getMethodName();
			for( Method m: clazz.getBenchmarkMethods() ) {
				if ( m.getName().equals( n ) ) {
					return true;
				}
			}
			return false;
		}
		String getClassName() {
			String x = "" + proto;
			x = x.substring( 0, x.lastIndexOf( '.' ) );
			return x;
		}
		String getMethodName() {
			String x = "" + proto;
			x = x.substring( 0, x.indexOf( '(' ) );
			x = x.substring( x.lastIndexOf( '.' ) + 1 );
			return x;
		}
		String getParameters() {
			String x = "" + proto;
			x = x.substring( x.indexOf( '[' ) );
			return x;
		}
		PolymorphicType[][] permute()
		throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
		{
			return Algorithm.permute( clazz.getParamFields().toArray( new Field[ 0 ] ) );
		}
		public static String toString( Field[] field, PolymorphicType[] pmt ) {
			if ( field.length != pmt.length ) {
				throw new IllegalArgumentException();
			}
			String r = "[";
			for( int i = 0; i < pmt.length; i++ ) {
				r += field[ i ].getName() + ":" + pmt[ i ];
				if ( i < pmt.length - 1 ) {
					r += ",";
				}
			}
			r += "]";
			return r;
		}
		AnnotatedClass getAnnotatedClass() {
			return clazz;
		}
		public IStatistics getStatistics() {
			return is;
		}
	}

	IStatistics getStats( ArrayList<Trial> trials ) {
		OnlineStatistics os = new OnlineStatistics();
		for( Trial t: trials ) {
			for( Measurement m: t.measurements() ) {
				if ( "elapsed_time_ns".equals( m.description() ) ) {
					os.update( (double)(Double) m.value().value );
				}
			}
		}
		return os;
	}

	private TreeMap<UUID,Crunched> filterByMicro( TreeMap<UUID,Crunched> tmc, boolean micro ) {
		TreeMap<UUID,Crunched> r = new TreeMap<UUID,Crunched>();
		for( Map.Entry<UUID,Crunched> e: tmc.entrySet() ) {
			UUID key = e.getKey();
			Crunched val = e.getValue();
			if ( val.isMicro() && micro ) {
				r.put( key, val );
			} else if ( !( val.isMicro() || micro ) ) {
				r.put( key, val );
			}
		}
		return r;
	}

	private TreeMap<UUID,Crunched> filterByParameters( TreeMap<UUID,Crunched> tmc, String param ) {
		TreeMap<UUID,Crunched> r = new TreeMap<UUID,Crunched>();
		for( Map.Entry<UUID,Crunched> e: tmc.entrySet() ) {
			UUID key = e.getKey();
			Crunched val = e.getValue();
			if ( param.equals( val.getParameters() ) ) {
				r.put( key, val );
			}
		}
		return r;
	}

	private TreeMap<UUID,Crunched> filterByMethodName( TreeMap<UUID,Crunched> tmc, String method_name ) {
		TreeMap<UUID,Crunched> r = new TreeMap<UUID,Crunched>();
		for( Map.Entry<UUID,Crunched> e: tmc.entrySet() ) {
			UUID key = e.getKey();
			Crunched val = e.getValue();
			if ( method_name.equals( val.getMethodName() ) ) {
				r.put( key, val );
			}
		}
		return r;
	}

	private Class<?>[] extractClasses( TreeMap<UUID,Crunched> micro, TreeMap<UUID,Crunched> macro ) {
		ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
		for( Crunched c: micro.values() ) {
			Class<?> k = c.getAnnotatedClass().getAnnotatedClass();
			if ( ! classes.contains( k ) ) {
				classes.add( k );
			}
		}
		for( Crunched c: macro.values() ) {
			Class<?> k = c.getAnnotatedClass().getAnnotatedClass();
			if ( ! classes.contains( k ) ) {
				classes.add( k );
			}
		}
		return classes.toArray( new Class<?>[0] );
	}

	private static final String sep = "-";
	private String genFileName( Crunched c, String param_str ) {
		SimpleDateFormat fmt = new SimpleDateFormat( "yyyyMMdd" );
		String date_str = fmt.format( date );
		String epoch_str = "" + epoch;
		param_str = param_str.substring( 1, param_str.length() -1 );
		String r = output_directory + File.separator + "sliderule" + sep + date_str + sep + epoch_str + sep + param_str + ".html";
		return r;
	}

	private String[] unpackFileName( String file_name ) {
		String[] foo = file_name.split( File.separator );
		file_name = foo[ foo.length - 1 ];
		file_name = file_name.substring( 0, file_name.length() - ".html".length() );
		foo = file_name.split( sep );
		String[] r = file_name.split( sep );
		return r;
	}

	private static final String head =
		"<html>" + "\n" +
		"  <head>" + "\n" +
	    "    <script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>" + "\n" +
	    "    <script type=\"text/javascript\">" + "\n" +
	    "      google.load(\"visualization\", \"1\", {packages:[\"corechart\"]});" + "\n" +
	    "      google.setOnLoadCallback(drawVisualization);" + "\n" +
	    "      function drawVisualization() {" + "\n"
	;
	private void composeHead( PrintWriter pw ) {
		pw.print( head );
	}
	private static final String tail[] = {
		"    }"       + "\n" +
		"  </script>" + "\n" +
		"</head>"     + "\n" +
		"<body>"      + "\n",
		"</body>"     + "\n" +
		"</html>"     + "\n",
	};
	private void composeTail( PrintWriter pw, String title, String[] param, int n ) {
		pw.print( tail[ 0 ] );

		pw.println( "  <p><big><b>" + title + "</b></big></p>" );
		pw.println( "  <p><b>Parameter List:</b><br/>" );
		pw.println( "  <ul>" );
		for( int i=0; i<param.length; i++ ) {
			pw.println( "    <li>" + param[ i ] + "</li>" );
		}
		pw.println( "  </ul>" );
		pw.println( "  </p>" );

		for( int i = 0; i < n; i++ ) {
			pw.println( "  <div id=\"chart" + i + "\"></div>" );
		}
		pw.print( tail[ 1 ] );
	}

	private final static String[] chart_data_str = {
		"var data",
		" = google.visualization.arrayToDataTable([",
		"]);",
	};
	private final static String[] chart_opt_str = {
		"var options",
		" = {",
		"};",
	};
	private void doChart( PrintWriter pw, boolean micro, int chart_idx, TreeMap<UUID,Crunched> crunch ) {

		if ( crunch.size() >= 1 ) {

			// order by method name
			TreeMap<String,Crunched> sorted = new TreeMap<String,Crunched>();
			for( Crunched c: crunch.values() ) {
				sorted.put( c.getMethodName(), c );
			}

			// print chart data
			pw.println( "      " + chart_data_str[ 0 ]  + chart_idx + chart_data_str[ 1 ] );
			pw.println( "        " + "[ 'Test', 'Time (ns)' ]," );
			int i=0;
			for( Crunched c: sorted.values() ) {
				pw.print( "        " + "[ '" + c.getMethodName() + "', " + c.getStatistics().mean() + " ]" );
				if ( i < sorted.size() - 1 ) {
					pw.print(",");
				}
				pw.println();
			}
			pw.println( "      " + chart_data_str[ 2 ] );

			// print chart options
			pw.println( "      " + chart_opt_str[ 0 ] + chart_idx + chart_opt_str[ 1 ] );
			pw.println( "        " + "title: \"" + (micro ? "Micro-Benchmark" : "Macro-Benchmark") + " Execution Time (ns)\"" );
			pw.println( "      " + chart_opt_str[ 2 ] );

			// print code to draw chart
			pw.println( "      " + "var chart" + chart_idx + " = new google.visualization.BarChart(document.getElementById(\"chart" + chart_idx + "\"));" );
			pw.println( "      " + "var view"  + chart_idx + " = new google.visualization.DataView(data" + chart_idx + ");" );
			pw.println( "      " + "chart"     + chart_idx + ".draw(view" + chart_idx + ", options" + chart_idx + ");" );
		}
	}

	private void doMultiChart( PrintWriter pw, boolean micro, int chart_idx, TreeMap<UUID,Crunched> crunch, String method_name ) {

		if ( crunch.size() >= 1 ) {

			// order by class name
			TreeMap<String,Crunched> sorted = new TreeMap<String,Crunched>();
			for( Crunched c: crunch.values() ) {
				sorted.put( c.getClassName(), c );
			}

			// print chart data
			pw.println( "      " + chart_data_str[ 0 ]  + chart_idx + chart_data_str[ 1 ] );
			pw.println( "        " + "[ 'Class', 'Time (ns)' ]," );
			int i=0;
			for( Crunched c: sorted.values() ) {
				pw.print( "        " + "[ '" + c.getClassName() + "', " + c.getStatistics().mean() + " ]" );
				if ( i < sorted.size() - 1 ) {
					pw.print(",");
				}
				pw.println();
			}
			pw.println( "      " + chart_data_str[ 2 ] );

			// print chart options
			pw.println( "      " + chart_opt_str[ 0 ] + chart_idx + chart_opt_str[ 1 ] );
			pw.println( "        " + "title: \"" + (micro ? "Micro-Benchmark" : "Macro-Benchmark") + " Execution Time of " + ( method_name + "()" ) + " (ns)\"" );
			pw.println( "      " + chart_opt_str[ 2 ] );

			// print code to draw chart
			pw.println( "      " + "var chart" + chart_idx + " = new google.visualization.BarChart(document.getElementById(\"chart" + chart_idx + "\"));" );
			pw.println( "      " + "var view"  + chart_idx + " = new google.visualization.DataView(data" + chart_idx + ");" );
			pw.println( "      " + "chart"     + chart_idx + ".draw(view" + chart_idx + ", options" + chart_idx + ");" );
		}
	}

	private String[] extractUniqueMethodNames( TreeMap<UUID,Crunched> crunch ) {
		ArrayList<String> als = new ArrayList<String>();
		for( Crunched c: crunch.values() ) {
			String mn = c.getMethodName();
			if ( ! als.contains( mn ) ) {
				als.add( mn );
			}
		}
		return als.toArray( new String[0] );
	}

	private void composeFile( String file_name, PrintWriter pw, TreeMap<UUID,Crunched> micro, TreeMap<UUID,Crunched> macro ) {

		String[] file_name_unpacked = unpackFileName( file_name );
		String date = file_name_unpacked[ 1 ];
		String[] param = file_name_unpacked[ 3 ].split( "," );
		Arrays.sort( param );

		Class<?>[] classes = extractClasses( micro, macro );

		composeHead( pw );

		boolean multi_class = 1 != classes.length;
		int chart_idx = 0;

		if ( multi_class ) {
			String[] unique_method_names;
			unique_method_names = extractUniqueMethodNames( micro );
			Arrays.sort( unique_method_names );
			for( String method_name: unique_method_names ) {
				doMultiChart( pw, true, chart_idx++, filterByMethodName( micro, method_name ), method_name );
			}
			unique_method_names = extractUniqueMethodNames( macro );
			Arrays.sort( unique_method_names );
			for( String method_name: unique_method_names ) {
				doMultiChart( pw, false, chart_idx++, filterByMethodName( macro, method_name ), method_name );
			}
		} else {
			doChart( pw, true, chart_idx++, micro );
			doChart( pw, false, chart_idx++, macro );
		}

		if ( plot_histogram ) {
			System.err.println( "histogram plotting is not yet implemented" );
		}

		String title = ( multi_class ? "Multi-Class" : classes[0].getName() ) + " Benchmark Results " + date + "<br/>(smaller is better)";

		composeTail( pw, title, param, chart_idx );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {

		// this is really the only indication that we have processed all of the trials, so...

		// first, calculate all of the statistics for each experiment
		TreeMap<UUID,Crunched> tmc = new TreeMap<UUID,Crunched>();
		for( Map.Entry<UUID,ArrayList<Trial>> e: trial_set.entrySet() ) {
			IStatistics is = getStats( e.getValue() );
			tmc.put( e.getKey(), new Crunched( e.getValue().get( 0 ), is ) );
		}

		// second, separate the trials into two groups - micro and macro
		TreeMap<UUID,Crunched> micro = filterByMicro( tmc , true );
		TreeMap<UUID,Crunched> macro = filterByMicro( tmc , false );

		Crunched c = tmc.firstEntry().getValue();
		PolymorphicType[][] pmt;
		try {
			pmt = c.permute();
		} catch ( Exception e1 ) {
			throw new IllegalStateException( "unable to permute" );
		}
		for( int row = 0; row < pmt.length; row++ ) {
			String pmt_str = Crunched.toString( c.getAnnotatedClass().getParamFields().toArray( new Field[ 0 ] ), pmt[ row ] );

			TreeMap<UUID,Crunched> these_micro = filterByParameters( micro, pmt_str );
			TreeMap<UUID,Crunched> these_macro = filterByParameters( macro, pmt_str );

			String fn = genFileName( c, pmt_str );
			File f = new File( fn );
			f.mkdirs();
			if ( f.exists() ) {
				f.delete();
			}
			PrintWriter pw = new PrintWriter( new FileOutputStream( f ) );
			composeFile( fn, pw, these_micro, these_macro );
			pw.close();
		}
	}

	Date date = new Date();
	long epoch = System.currentTimeMillis();

	static {
		String s;
		boolean b;
		s = Arguments.static_config_properties.getProperty( "org.sliderule.runner.GoogleChartsResultProcessor.output.directory" );
		output_directory = null == s ? new File( "." ).getAbsolutePath() : s;
		s = Arguments.static_config_properties.getProperty( "org.sliderule.runner.GoogleChartsResultProcessor.plot.histogram" );
		b = Boolean.parseBoolean( s );
		plot_histogram = b;
	}
}
