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

public class Factorial {
	
	static long recursive( int number ) {
		switch ( checkValidNumber( number ) ) {
		case 0:
			return 1;
		default:
			return recursive( number - 1 ) * number;
		}
	}

	static long iterative( int number ) {
		checkValidNumber( number );
		long result = 1;
		for ( int i = number; i > 0; i-- ) {
			result *= i;
		}
		return result;
	}

	private static int checkValidNumber( int number ) {
		if ( number < 0 ) {
			throw new IllegalArgumentException();
		}
		return number;
	}
}
