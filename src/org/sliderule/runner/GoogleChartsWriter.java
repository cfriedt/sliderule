package org.sliderule.runner;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.sliderule.model.*;
import org.sliderule.stats.*;

class GoogleChartsWriter {

	// InMemoryResultProcessor.

	final String date;
	final PrintWriter pw;
	final TreeMap<UUID,ArrayList<Trial>> trials;
	final boolean sweep;
	final TreeSet<Class<?>> classes;
	final TreeSet<Method> methods;
	final boolean multi_class;
	final SlideRuleAnnotations ann;

	public GoogleChartsWriter( String date, OutputStream os, TreeMap<UUID,ArrayList<Trial>> trials ) {
		this.date = date;
		this.pw = new PrintWriter( os );
		this.trials = trials;
		sweep = GoogleChartsResultProcessor.trialsAreParametricSweep( trials );

		methods = uniqueMethods();
		classes = uniqueClasses();
		multi_class = classes.size() != 1;
		ann = ( (SimpleTrial) trials.firstEntry().getValue().get(  0  ) ).getSlideRuleAnnotations();
	}

	String foo0 =
	"<html>" + "\n" +
	"  <head>" + "\n" +
	"    <script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>" + "\n" +
	"    <script type=\"text/javascript\">" + "\n" +
	"      google.load(\"visualization\", \"1.1\", {packages:[\"bar\"]});" + "\n" +
	"      google.setOnLoadCallback(drawVisualization);" + "\n" +
	"      function drawVisualization() {";
	String foo7 =
	"    }" + "\n" +
	"    </script>" + "\n" +
	"  </head>" + "\n" +
	"  <body>";
	String foo8 =
	"  </body>" + "\n" +
	"</html>";

	PolymorphicType[] uniqueValues( int idx ) {
		TreeSet<PolymorphicType> pmt = new TreeSet<PolymorphicType>();
		for( ArrayList<Trial> alt: trials.values() ) {
			Trial t = alt.get( 0 );
			SimpleTrial st = (SimpleTrial) t;
			PolymorphicType[] v = st.getParamValue();
			pmt.add( v[ idx ] );
		}
		return pmt.toArray( new PolymorphicType[ 0 ] );
	}
	TreeSet<Method> uniqueMethods() {
		TreeSet<Method> methods = new TreeSet<Method>( new SlideRuleAnnotations.MemberComparator() );
		for( ArrayList<Trial> alt: trials.values() ) {
			Trial t = alt.get( 0 );
			SimpleTrial st = (SimpleTrial) t;
			Method m = st.getMethod();
			methods.add( m );
		}
		return methods;
	}
	TreeSet<Class<?>> uniqueClasses() {
		TreeSet<Class<?>> classes = new TreeSet<Class<?>>( new SlideRuleAnnotations.ClassComparator() );
		for( ArrayList<Trial> alt: trials.values() ) {
			Trial t = alt.get( 0 );
			SimpleTrial st = (SimpleTrial) t;
			Class<?> c = st.getSlideRuleAnnotations().getAnnotatedClass();
			classes.add( c );
		}
		return classes;
	}

	int doSimpleChart( boolean micro, int chart_idx ) {
		int i;

		SimpleTrial prototype = (SimpleTrial) trials.firstEntry().getValue().get( 0 );

		TreeMap<UUID,ArrayList<Trial>> subset1, subset2;

		subset1 = InMemoryResultProcessor.filterByMicro( trials, micro );
		if ( subset1.isEmpty() ) {
			return 0;
		}

		Set<Method> meth = micro ? ann.getBenchmarkMethods() : ann.getMacrobenchmarkMethods();

		pw.println( "      var data" + chart_idx + " = google.visualization.arrayToDataTable([" );
		pw.print( "        [ 'Test', " );
		i=0;
		for( Method m: meth ) {
			pw.print( "'" + m.getName() + "'" );
			if ( i < meth.size() - 1 ) {
				pw.print( "," );
			}
			i++;
		}
		pw.println( " ]," );

		pw.print( "        [ 'Mean Time (ns)', " );

		i=0;
		for( Method m: meth ) {
			subset2 = InMemoryResultProcessor.filterByMethod( subset1, m );
			ArrayList<Trial> alt = subset2.firstEntry().getValue();
			OfflineStatistics os = new OfflineStatistics( SimpleTrial.extractMeans( alt ) );
			pw.print( "'" + os.mean() + "'" );
			if ( i < methods.size() - 1 ) {
				pw.print( "," );
			}
			i++;
		}
		pw.println( " ]" );

		pw.println( "      ]);" );

		pw.println( "      var options" + chart_idx + " = {" );
		pw.println( "        chart: {" );
		pw.println( "          title: '" + classes.first().getName() + " " + ( micro ? "Micro" : "Macro" ) + "-Benchmark, " + date + "'," );
		pw.println( "          subtitle: 'Parameters: " + PolymorphicType.nameParams( prototype.getParam(), prototype.getParamValue() ) + "'" );
		pw.println( "        }," );
		pw.println( "        bars: 'horizontal' // Required for Material Bar Charts." );
		pw.println( "      };" );

		pw.println( "      var chart" + chart_idx + " = new google.charts.Bar(document.getElementById('chart" + chart_idx + "'));" );

		pw.println( "      chart" + chart_idx + ".draw(data" + chart_idx + ", " + "options" + chart_idx + ");" );
		return 1;
	}

	int doMultiChart( boolean micro, int chart_idx ) {

		int i, j;
		Set<Method> meth;
		SimpleTrial prototype = (SimpleTrial) trials.firstEntry().getValue().get( 0 );

		TreeMap<UUID,ArrayList<Trial>> subset1, subset2, subset3;

		subset1 = InMemoryResultProcessor.filterByMicro( trials, micro );
		if ( subset1.isEmpty() ) {
			return 0;
		}

		pw.println( "      var data" + chart_idx + " = google.visualization.arrayToDataTable([" );
		pw.print( "        [ 'Class', " );

		i = 0;
		meth = micro ? ann.getBenchmarkMethods() : ann.getMacrobenchmarkMethods();
		for( Method m: meth ) {
			pw.print( "'" + m.getName() + "'" );
			if ( i < meth.size() - 1 ) {
				pw.print( ", " );
			}
		}
		pw.println( " ]," );

		j=0;
		for( Class<?> c: classes ) {
			subset2 = InMemoryResultProcessor.filterByClass( subset1, c );

			pw.print( "[ '" + c.getName() + "', " );

			i=0;
			for( Method m: meth ) {

				subset3 = InMemoryResultProcessor.filterByMethod( subset2, m );

				if ( 1 != subset3.size() ) {
					throw new IllegalStateException();
				}

				ArrayList<Trial> alt = subset3.firstEntry().getValue();
				OfflineStatistics os = new OfflineStatistics( SimpleTrial.extractMeans( alt ) );
				pw.print( "'" + os.mean() + "'" );
				if ( i < meth.size() - 1 ) {
					pw.print( "," );
				}
				i++;
			}
			pw.print( " ]" );
			if ( j < classes.size() - 1 ) {
				pw.print( "," );
			}
			pw.println();
			j++;
		}

		pw.println( "]);" );

		pw.println( "      var options" + chart_idx + " = {" );
		pw.println( "        chart: {" );
		pw.println( "          title: '" + ( micro ? "Micro" : "Macro" ) + "-Benchmark, Mean Time (ns), " + date + "'," );
		pw.println( "          subtitle: 'Parameters: " + PolymorphicType.nameParams( prototype.getParam(), prototype.getParamValue() ) + "'" );
		pw.println( "        }," );
		pw.println( "        bars: 'horizontal' // Required for Material Bar Charts." );
		pw.println( "      };" );

		pw.println( "      var chart" + chart_idx + " = new google.charts.Bar(document.getElementById('chart" + chart_idx + "'));" );

		pw.println( "      chart" + chart_idx + ".draw(data" + chart_idx + ", " + "options" + chart_idx + ");" );
		chart_idx++;

		return 1;
	}

	void writeSimple() {

		int chart_idx = 0;

		pw.println( foo0 );

		if ( multi_class ) {

			chart_idx += doMultiChart( true, chart_idx );
			chart_idx += doMultiChart( false, chart_idx );

		} else {

			chart_idx += doSimpleChart( true, chart_idx );
			chart_idx += doSimpleChart( false, chart_idx );

		}

		pw.println( foo7 );
		for( int i = 0; i < chart_idx; i++ ) {
			pw.println( "    <div id=\"chart" + i + "\" style=\"width: 900px; height: 500px;\"></div>" );
//			pw.println( "    <div id=\"chart" + i + "\"></div>" );
//			pw.println( "    <div id=\"chart" + i + "\" style=\"width: 70%;\"></div>" );
		}
		pw.println( foo8 );
	}


	int doSimpleSweepChart( boolean micro, int chart_idx ) {
		int i, j;

		SimpleTrial prototype = (SimpleTrial) trials.firstEntry().getValue().get( 0 );

		TreeMap<UUID,ArrayList<Trial>> subset1, subset2, subset3;

		subset1 = InMemoryResultProcessor.filterByMicro( trials, micro );
		if ( subset1.isEmpty() ) {
			return 0;
		}

		int sweep_parameter = GoogleChartsResultProcessor.parametricSweepIndex( trials );
		Field sweep_field = ann.getParamFields().toArray( new Field[ 0 ] )[ sweep_parameter ];
		Field[] base_name = GoogleChartsResultProcessor.extractOneFromArray( prototype.getParam(), sweep_parameter );
		PolymorphicType[] base_case = GoogleChartsResultProcessor.extractOneFromArray( prototype.getParamValue(), sweep_parameter );

		PolymorphicType[] sweep_values = uniqueValues( sweep_parameter );

		Set<Method> meth = micro ? ann.getBenchmarkMethods() : ann.getMacrobenchmarkMethods();

		pw.println( "      var data" + chart_idx + " = google.visualization.arrayToDataTable([" );
		pw.print( "        [ '" + sweep_field.getName() + "', " );
		i=0;
		for( Method m: meth ) {
			pw.print( "'" + m.getName() + "'" );
			if ( i < meth.size() - 1 ) {
				pw.print( "," );
			}
			i++;
		}
		pw.println( " ]," );

		j=0;
		for( PolymorphicType sweep_pmt: sweep_values ) {

			subset2= InMemoryResultProcessor.filterByParamValue( subset1, sweep_field, sweep_pmt );

			pw.print( "        [ '" + sweep_pmt.value + "', " );

			i=0;
			for( Method m: meth ) {

				subset3 = InMemoryResultProcessor.filterByMethod( subset2, m );

				ArrayList<Trial> alt = subset3.firstEntry().getValue();

				OfflineStatistics os = new OfflineStatistics( SimpleTrial.extractMeans( alt ) );

				pw.print( os.mean() );
				if ( i < methods.size() - 1 ) {
					pw.print( "," );
				}
				i++;
			}
			pw.print( " ]" );
			if ( j < sweep_values.length - 1 ) {
				pw.print( "," );
			}
			j++;
		}

		pw.println( "      ]);" );

		pw.println( "      var options" + chart_idx + " = {" );
		pw.println( "        chart: {" );
		pw.println( "          title: '" + classes.first().getName() + " " + ( micro ? "Micro" : "Macro" ) + "-Benchmark, " + date + "'," );
		pw.println( "          subtitle: 'Parameters: " + PolymorphicType.nameParams( base_name, base_case ) + "'" );
		pw.println( "        }," );
		pw.println( "        bars: 'horizontal' // Required for Material Bar Charts." );
		pw.println( "      };" );

		pw.println( "      var chart" + chart_idx + " = new google.charts.Bar(document.getElementById('chart" + chart_idx + "'));" );

		pw.println( "      chart" + chart_idx + ".draw(data" + chart_idx + ", " + "options" + chart_idx + ");" );
		return 1;
	}

	int doMultiSweepChart( boolean micro, int chart_idx ) {

		int i, j;
		TreeMap<UUID,ArrayList<Trial>> subset1, subset2, subset3, subset4;

		SimpleTrial prototype = (SimpleTrial) trials.firstEntry().getValue().get( 0 );

		int sweep_parameter = GoogleChartsResultProcessor.parametricSweepIndex( trials );
		Field sweep_field = ann.getParamFields().toArray( new Field[ 0 ] )[ sweep_parameter ];
		Field[] base_name = GoogleChartsResultProcessor.extractOneFromArray( prototype.getParam(), sweep_parameter );
		PolymorphicType[] base_case = GoogleChartsResultProcessor.extractOneFromArray( prototype.getParamValue(), sweep_parameter );
		PolymorphicType[] sweep_values = uniqueValues( sweep_parameter );
		Set<Method> meth = micro ? ann.getBenchmarkMethods() : ann.getMacrobenchmarkMethods();

		subset1 = InMemoryResultProcessor.filterByMicro( trials, micro );

		TreeSet<Class<?>> classes = uniqueClasses();

		// one chart per method
		for( Method m: meth ) {

			pw.println( "      var data" + chart_idx + " = google.visualization.arrayToDataTable([" );
			pw.print( "        [ '" + sweep_field.getName() + "', " );

			i=0;
			for( Class<?> clazz: classes ) {
				pw.print( "'" + clazz.getName() + "'" );
				if ( i < classes.size() - 1 ) {
					pw.print( "," );
				}
			}
			pw.println( "], " );

			subset2 = InMemoryResultProcessor.filterByMethod( subset1, m );

			i=0;
			// for each sweep value
			for( PolymorphicType sv: sweep_values ) {

				subset3 = InMemoryResultProcessor.filterByParamValue( subset2, sweep_field, sv );

				pw.print( "        [ '" + sv.value + "', " );

				j=0;
				// for each class
				for( Class<?> clazz: classes ) {

					subset4 = InMemoryResultProcessor.filterByClass( subset3, clazz );

					ArrayList<Trial> alt = subset4.firstEntry().getValue();

					OfflineStatistics os = new OfflineStatistics( SimpleTrial.extractMeans( alt ) );

					pw.print( os.mean() );

					if ( j < classes.size() - 1 ) {
						pw.print( "," );
					}
				}

				pw.print( "]" );
				if ( i < sweep_values.length - 1 ) {
					pw.print( "," );
				}
				pw.println();
			}

			pw.println( "      ]);" );

			pw.println( "      var options" + chart_idx + " = {" );
			pw.println( "        chart: {" );
			pw.println( "          title: '" + m.getName() + "() "  + ( micro ? "Micro" : "Macro" ) + "-Benchmark (ns), " + date + "'," );
			pw.println( "          subtitle: 'Parameters: " + PolymorphicType.nameParams( base_name, base_case ) + "'" );
			pw.println( "        }," );
			pw.println( "        bars: 'horizontal' // Required for Material Bar Charts." );
			pw.println( "      };" );

			pw.println( "      var chart" + chart_idx + " = new google.charts.Bar(document.getElementById('chart" + chart_idx + "'));" );

			pw.println( "      chart" + chart_idx + ".draw(data" + chart_idx + ", " + "options" + chart_idx + ");" );

			chart_idx++;
		}

		return chart_idx;
	}

	void writeSweep() {

		int chart_idx = 0;

		pw.println( foo0 );

		if ( multi_class ) {

			chart_idx += doMultiSweepChart( true, chart_idx );
			chart_idx += doMultiSweepChart( false, chart_idx );

		} else {

			chart_idx += doSimpleSweepChart( true, chart_idx );
			chart_idx += doSimpleSweepChart( false, chart_idx );

		}

		pw.println( foo7 );
		for( int i = 0; i < chart_idx; i++ ) {
			pw.println( "    <div id=\"chart" + i + "\" style=\"width: 900px; height: 500px;\"></div>" );
//			pw.println( "    <div id=\"chart" + i + "\"></div>" );
//			pw.println( "    <div id=\"chart" + i + "\" style=\"width: 70%;\"></div>" );
		}
		pw.println( foo8 );
	}

	public void write()
	{
		if ( sweep ) {
			writeSweep();
		} else {
			writeSimple();
		}
	}

	public void close()
	{
		pw.close();
	}
}
