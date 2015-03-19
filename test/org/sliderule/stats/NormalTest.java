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
import org.sliderule.stats.*;

public class NormalTest {
	@Test
	public void cdf() {
		int n;
		double x;
		double expected_confidence;
		double actual_confidence;
		double epsilon = 0.0001;
		
		n = 8;
		x = 0.5;
		expected_confidence = 0.691462;
		actual_confidence = Normal.cdf( n, x );
		assertEquals( "Pr( X <= " + x + " ) = " + expected_confidence, expected_confidence, actual_confidence, epsilon );

		n = 8;
		x = 1;
		expected_confidence = 0.841345;
		actual_confidence = Normal.cdf( n, x );
		assertEquals( "Pr( X <= " + x + " ) = " + expected_confidence, expected_confidence, actual_confidence, epsilon );

		n = 8;
		x = 2;
		expected_confidence = 0.977250;
		actual_confidence = Normal.cdf( n, x );
		assertEquals( "Pr( X <= " + x + " ) = " + expected_confidence, expected_confidence, actual_confidence, epsilon );

		n = 8;
		x = 3;
		expected_confidence = 0.99865;
		actual_confidence = Normal.cdf( n, x );
		assertEquals( "Pr( X <= " + x + " ) = " + expected_confidence, expected_confidence, actual_confidence, epsilon );

		n = 8;
		x = 4;
		expected_confidence = 1;
		actual_confidence = Normal.cdf( n, x );
		assertEquals( "Pr( X <= " + x + " ) = " + expected_confidence, expected_confidence, actual_confidence, epsilon );
	}
	@Test
	public void inv() {
		int n;
		double p;
		double epsilon = 0.01;
		double expected_variable;
		double actual_variable;
		
		n = 8;
		p = 0.691462;
		expected_variable = 0.5;
		actual_variable = Normal.inv( n, p );
		assertEquals( "Pr( X <= " + expected_variable + " ) = " + p, expected_variable, actual_variable, epsilon );

		n = 8;
		p = 0.841345;
		expected_variable = 1;
		actual_variable = Normal.inv( n, p );
		assertEquals( "Pr( X <= " + expected_variable + " ) = " + p, expected_variable, actual_variable, epsilon );

		n = 8;
		p = 0.977250;
		expected_variable = 2;
		actual_variable = Normal.inv( n, p );
		assertEquals( "Pr( X <= " + expected_variable + " ) = " + p, expected_variable, actual_variable, epsilon );

		n = 8;
		p = 0.99865;
		expected_variable = 3;
		actual_variable = Normal.inv( n, p );
		assertEquals( "Pr( X <= " + expected_variable + " ) = " + p, expected_variable, actual_variable, epsilon );

		n = 8;
		expected_variable = 4;
		p = 1;
		actual_variable = Normal.inv( n, p );
		assertEquals( "Pr( X <= " + expected_variable + " ) = " + p, expected_variable, actual_variable, epsilon );
	}
	@Ignore
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
		expected_variable = 2.364624;
		actual_variable = Normal.inv( n, Normal.cdf( n, expected_variable ) );
		assertEquals( "inv( cdf( " + expected_variable + " ) = " + expected_variable, expected_variable, actual_variable, variable_epsilon );

		n = 8;
		expected_confidence = 0.95;
		actual_confidence = Normal.cdf( n, Normal.inv( n, expected_confidence ) );
		assertEquals( "cdf( inv( " + expected_confidence + " ) = " + expected_confidence, expected_confidence, actual_confidence, confidence_epsilon );
	}
}
