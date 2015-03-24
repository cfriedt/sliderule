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

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

public class AlgorithmTest {
	@Test
	public void euprod() {
		int cardinality[] = new int[] { 3, 4, 2 };
		int expected_permutations;
		int actual_permutations;

		// We can calculate the total number of permutations by computing the product of
		// the cardinality vector.
		// This is similar to calculating the number of permutations for an N-bit binary
		// word (2*2*2..2=2^N permutations), except in this case, there are not
		// necessarily 2 possibilities per item
		expected_permutations = 24; // cardinality[0] * cardinality[1] * cardinality[2];
		actual_permutations = Algorithm.euprod( cardinality );
		assertEquals( "there should be " + expected_permutations + " permutations for values " + Arrays.asList( cardinality ), expected_permutations, actual_permutations  );

		// We can generate the total number of permutations per item by
		// holding the euclidian product at a certain position - i.e
		// the same methods that we use to construct a truth table.
		//
		// This allows us to relatively easily construct / generate
		// a number of permutations using two loops rather than using
		// recursion.
		//
		// e.g. cardinality := { 3, 4, 2 }
		//                       a  d  h
		//                       b  e  i
		//                       c  f
		//                          g
		//
		// "Truth table"
		// =============
		// 0: a d h
		// 1: a d i
		// 2: a e h
		// 3: a e i
		// 4: a f h
		// 5: a f i
		// 6: a g h
		// 7: a g i
		// 8: b d h
		// ...
		// 23: c g i
		//
		// if we hold position 0, then item 1 would would be repeated 4 * 2 times
		// to exhaust the permutations of the remaining items.
		expected_permutations = 8; // cardinality[1] * cardinality[2];
		actual_permutations = Algorithm.euprod( cardinality, 0 );
		assertEquals( "there should be " + expected_permutations + " permutations for values " + Arrays.asList( cardinality ), expected_permutations, actual_permutations  );

		expected_permutations = 2; // cardinality[2];
		actual_permutations = Algorithm.euprod( cardinality, 1 );
		assertEquals( "there should be " + expected_permutations + " permutations for values " + Arrays.asList( cardinality ), expected_permutations, actual_permutations  );

		expected_permutations = 1;
		actual_permutations = Algorithm.euprod( cardinality, 2 );
		assertEquals( "there should be " + expected_permutations + " permutations for values " + Arrays.asList( cardinality ), expected_permutations, actual_permutations  );
	}
}
