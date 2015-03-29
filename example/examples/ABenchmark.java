package examples;

import org.sliderule.*;
import org.sliderule.api.*;

abstract class ABenchmark {
	AOperation operation;
	protected ABenchmark( AOperation operation ) {
		this.operation = operation;
	}

	@Param({
		"5",
//		"10",
//		"20",
	}) int number; // -Dnumber=1,2,3

	@Benchmark
	long iterative( int reps ) {
		final AOperation operation = this.operation;
		int number = this.number;
		long dummy = 0L;
		for( int i=0; i < reps; i++ ) {
			dummy |= operation.iterative( number );
		}
		return dummy;
	}

	@Benchmark
	long recursive( int reps ) {
		final AOperation operation = this.operation;
		int number = this.number;
		long dummy = 0L;
		for( int i=0; i < reps; i++ ) {
			dummy |= operation.recursive( number );
		}
		return dummy;
	}

	@Macrobenchmark
	long macroIterative() {
		final AOperation operation = this.operation;
		int number = this.number;
		int reps = 10000;
		long dummy = 0L;
		for( int i=0; i < reps; i++ ) {
			dummy += operation.iterative( number );
		}
		return dummy;
	}

	@Macrobenchmark
	long macroRecursive() {
		AOperation operation = this.operation;
		int number = this.number;
		int reps = 10000;
		long dummy = 0L;
		for( int i=0; i < reps; i++ ) {
			dummy += operation.recursive( number );
		}
		return dummy;
	}
}
