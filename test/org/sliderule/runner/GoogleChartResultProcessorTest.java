package org.sliderule.runner;

import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.*;

import org.junit.*;
import org.sliderule.model.*;
import org.sliderule.stats.*;

public class GoogleChartResultProcessorTest {

	static Class<?> clazz;
	static Method m;
	static Field f;
	static PolymorphicType pmt;
	static Trial t;
	static double[] d;
	static GoogleChartsResultProcessor.Crunched crunch;

	@BeforeClass
	public static void setup() {
		clazz = examples.FactorialBenchmark.class;
		m = null;
		ArrayList<Method> meth = new ArrayList<Method>();
		meth.addAll(  Arrays.asList( clazz.getMethods() ) );
		meth.addAll(  Arrays.asList( clazz.getDeclaredMethods() ) );
		for( Method em: meth ) {
			if ( em.getName().equals( "iterative" ) ) {
				m = em;
				break;
			}
		}
		f = null;
		ArrayList<Field> fie = new ArrayList<Field>();
		fie.addAll(  Arrays.asList( clazz.getFields() ) );
		fie.addAll(  Arrays.asList( clazz.getDeclaredFields() ) );
		for( Field ef: fie ) {
			if ( ef.getName().equals( "number" ) ) {
				f = ef;
				break;
			}
		}
		pmt = new PolymorphicType( int.class, "5" );
		t = new SimpleTrial( UUID.randomUUID(), clazz, m, new Field[] { f }, new PolymorphicType[] { pmt } );
		d = new double[] { 5, 10, 20 };
		crunch = new GoogleChartsResultProcessor.Crunched( t, new OfflineStatistics( d ) );
	}

	@Test
	public void getClassName() {
		String expected_value = clazz.getName();
		String actual_value = crunch.getClassName();
		assertEquals( expected_value, actual_value );
	}

	@Test
	public void getMethodName() {
		String expected_value = m.getName();
		String actual_value = crunch.getMethodName();
		assertEquals( expected_value, actual_value );
	}

	@Test
	public void getParameters() {
		String expected_value = "[number:int:5]";
		String actual_value = crunch.getParameters();
		assertEquals( expected_value, actual_value );
	}
}
