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

package org.sliderule.stats;

import static org.junit.Assert.*;

import org.junit.*;

public class ChiSquaredTest {
	@Test
	public void cdf() {
		int n;
		double x;
		double expected_confidence;
		double actual_confidence;
		double epsilon = 0.01;

		n = 8;
		x = 14.067140;
		expected_confidence = 0.95;
		actual_confidence = ChiSquared.cdf( n, x );
		assertEquals( "Pr( X <= " + x + " ) = " + expected_confidence, expected_confidence, actual_confidence, epsilon );

		n = 8;
		x = -14.067140;
		expected_confidence = (1 - 0.95); // symmetry about 0 at 50%
		actual_confidence = ChiSquared.cdf( n, x );
		assertEquals( "Pr( X <= " + x + " ) = " + expected_confidence, expected_confidence, actual_confidence, epsilon );
	}
	@Test
	public void inv() {
		int n;
		double confidence;
		double expected_variable;
		double actual_variable;
		double epsilon = 0.00001;

		n = 8;
		confidence = 0.95;
		expected_variable = 14.067140;
		actual_variable = ChiSquared.inv( n, confidence );
		assertEquals( "Pr( X <= " + expected_variable + " ) = " + confidence, expected_variable, actual_variable, epsilon );

		n = 8;
		confidence = (1 - 0.95);
		expected_variable = 2.16735; // symmetry about 0 at 50%
		actual_variable = ChiSquared.inv( n, confidence );
		assertEquals( "Pr( X <= " + expected_variable + " ) = " + confidence, expected_variable, actual_variable, epsilon );
	}
	@Test
	public void invertibility() {
		int n;
		double expected_confidence;
		double actual_confidence;
		double expected_variable;
		double actual_variable;
		double confidence_epsilon = 0.001;
		double variable_epsilon = 0.00001;

		// first, test inv( cdf( X ) ) = X
		n = 8;
		expected_variable = 14.067140;
		actual_variable = ChiSquared.inv( n, ChiSquared.cdf( n, expected_variable ) );
		assertEquals( "inv( cdf( " + expected_variable + " ) = " + expected_variable, expected_variable, actual_variable, variable_epsilon );

		n = 8;
		expected_confidence = 0.95;
		actual_confidence = ChiSquared.cdf( n, ChiSquared.inv( n, expected_confidence ) );
		assertEquals( "cdf( inv( " + expected_confidence + " ) = " + expected_confidence, expected_confidence, actual_confidence, confidence_epsilon );
	}
	@Test
	public void minSamples() {
		// example is right out of DeGroot, but DeGroot is completely wrong
		// p1 = Pr( |U| < 1/5 * sqrt( 21 ) ) = 0.8203 => DeGroot says this should be 0.64
		// p2 = Pr( (1-q)^2 * 21 < V < (1+q)^2 * 21 ) = 0.654 => DeGroot says this should be 0.78
		double p = 1D / 2D;
		double q = 1D / 5D;
		int expected_n = 14;
		int actual_n = ChiSquared.minSamples( q, p );
		assertEquals( "" + expected_n + " samples required for a confidence of " + p + " within " + q + " standard deviations", expected_n, actual_n );
	}
	@Test
	public void test() {
		// Data right out of a Kahn Academy lecture on YouTube
		// Restaurant owner tries to approximate his patronage
		// Day:        M  T  W  T  F  S  (Sun Closed)
		// Expected %: 10 10 15 20 30 15
		// Observed:   30 14 34 45 57 20 // 200
		// Expected:   20 20 30 40 60 30
		double proto[] = { 0.10, 0.10, 0.15, 0.20, 0.30, 0.15 };
		double observed[] = { 30, 14, 34, 45, 57, 20 }; // # of people
		double N = 0;
		for( int i=0; i<observed.length; i++ ) {
			N += observed[i];
		}
		double expected[] = new double[ observed.length ];
		for( int i=0; i<observed.length; i++ ) {
 			expected[ i ] = proto[ i ] * N;
		}
		boolean expected_pass = false;
		boolean actual_pass = ChiSquared.test( 0.95, expected, observed );
		assertEquals( "expected the test to " + ( expected_pass ? "pass" : "fail"), expected_pass, actual_pass );
	}
}
