package org.sliderule.runner;

import static org.junit.Assert.*;

import org.junit.*;

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
}
