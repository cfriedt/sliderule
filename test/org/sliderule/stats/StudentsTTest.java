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

import java.util.*;

import org.junit.*;
import org.sliderule.stats.*;

public class StudentsTTest {
	@Test
	public void cdf() {
		int n;
		double x;
		double expected_confidence;
		double actual_confidence;
		double epsilon = 0.01;
		
		n = 8;
		x = 2.364624;
		expected_confidence = 0.95;
		actual_confidence = StudentsT.cdf( n, x );
		assertEquals( "Pr( X <= " + x + " ) = " + expected_confidence, expected_confidence, actual_confidence, epsilon );

		n = 8;
		x = -2.364624;
		expected_confidence = (1 - 0.95); // symmetry about 0 at 50%
		actual_confidence = StudentsT.cdf( n, x );
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
		expected_variable = 2.364624;
		actual_variable = StudentsT.inv( n, confidence );
		assertEquals( "Pr( X <= " + expected_variable + " ) = " + confidence, expected_variable, actual_variable, epsilon );

		n = 8;
		confidence = (1 - 0.95);
		expected_variable = -2.364624; // symmetry about 0 at 50%
		actual_variable = StudentsT.inv( n, confidence );
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
		expected_variable = 2.364624;
		actual_variable = StudentsT.inv( n, StudentsT.cdf( n, expected_variable ) );
		assertEquals( "inv( cdf( " + expected_variable + " ) = " + expected_variable, expected_variable, actual_variable, variable_epsilon );

		n = 8;
		expected_confidence = 0.95;
		actual_confidence = StudentsT.cdf( n, StudentsT.inv( n, expected_confidence ) );
		assertEquals( "cdf( inv( " + expected_confidence + " ) = " + expected_confidence, expected_confidence, actual_confidence, confidence_epsilon );
	}
	@Test
	public void test() {
		// right out of Chapra
		int n = 24;
		double u = 6.6;
		double o = 0.097133;
		double confidence = 0.95;
		double expected_t = 2.068655;
		double epsilon_t = 0.001;
		double epsilon_bounds = 0.001;
		double actual_t = StudentsT.inv( n, confidence );
		assertEquals( "t was correct", expected_t, actual_t, epsilon_t );
		double[] expected_bounds = new double[] { 6.5590, 6.6410 };
		double[] actual_bounds = StudentsT.bounds( n, confidence, u, o );
		assertEquals( "actual_bounds[0] == " + expected_bounds[0], expected_bounds[0], actual_bounds[0], epsilon_bounds );
		assertEquals( "actual_bounds[1] == " + expected_bounds[1], expected_bounds[1], actual_bounds[1], epsilon_bounds );
		boolean expected_pass = true;
		boolean actual_pass = StudentsT.test( n, confidence, u, o ); 
		assertEquals( "mean of " + u + " is possible with " + confidence + " confidence", expected_pass, actual_pass );
	}
}
