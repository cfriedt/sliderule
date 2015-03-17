/*
 * Copyright (C) 2013 Google Inc.
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

package examples;

import org.sliderule.*;

public final class FactorialBenchmark {

	@Param({
		"5",
		"10",
		"20",
	}) int number; // -Dnumber=1,2,3

	@Param({
		"foo",
		"bar",
	}) String dummy_param1;

	@Param({
		"oof",
		"rab",
		"zab",
		"halb",
	}) String dummy_param2;

	@Benchmark
	long iterative( int reps ) {
		int number = this.number;
		long dummy = 0L;
		for( int i=0; i < reps; i++ ) {
			dummy |= Factorial.iterative( number );
		}
		return dummy;
	}

	@Benchmark
	long recursive( int reps ) {
		int number = this.number;
		long dummy = 0L;
		for( int i=0; i < reps; i++ ) {
			dummy |= Factorial.recursive( number );
		}
		return dummy;
	}

	@Benchmark
	long tailRecursive( int reps ) {
		int number = this.number;
		long dummy = 0L;
		for( int i=0; i < reps; i++ ) {
			dummy |= Factorial.tailRecursive( number, 1 );
		}
		return dummy;
	}
}
