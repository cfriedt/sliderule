package org.sliderule.runner;

import java.lang.reflect.*;
import java.util.*;

import org.sliderule.*;
import org.sliderule.runner.*;

public class Permute {

	private Permute() {}

	static int euprod( int[] cardinality ) {
		int result = 1;
		for( int i=0; i < cardinality.length; result *= cardinality[ i ], i++ );
		return result;
	}

	static int euprod_omit( int[] cardinality, int index_to_omit ) {
		int result = 1;
		for( int i=0; i < cardinality.length; result *= ( i <= index_to_omit ? 1 : cardinality[ i ] ), i++ );
		return result;
	}

	static void permute() {

		int[] cardinality = new int[] {
			3, /* a, b, c, */
			4, /* d, e, f, g, */
			2, /* h, i, */
		};

		int nrows = euprod( cardinality );
		int ncols = cardinality.length;
		int[][] foo = new int[ nrows ][ ncols ];

		for( int col=0; col < ncols; col++ ) {
			int val = 0;
			int reps = euprod_omit( cardinality, col );
			for( int row=0; row < nrows; row++ ) {
				foo[ row ] [ col ] = val;
				if ( 0 == ( (row+1) % reps ) ) {
					val++;
					val %= cardinality[ col ];
				}
			}
		}
	}

/*
	static void permute( Algorithm alg ) {
		AnnotatedClass proto;

		proto = alg.alcai.get( 0 ).klass;

		ArrayList<Field> param_fields = new ArrayList<Field>();

		param_fields.addAll( proto.getParamFields() );

		int n_param_fields = param_fields.size();
		int[] cardinality = new int[ param_fields.size() ];

		// calculate the number of permutations, record cardinality of each param
		int n_param_permutations = 1;
		for( int i=0; i < param_fields.size(); i++ ) {
			Field f = param_fields.get( i );
			Param param = f.getAnnotation( org.sliderule.Param.class );
			List<String> value_string = Arrays.asList( param.value() );
			cardinality[ i ] = value_string.size();
			n_param_permutations *= Algorithm.factorial( cardinality[ i ] );
		}

		int j=0;
		for( int m=0; m<n_param_fields; m++ ) {
			for( int n=0; n<n_param_fields; n++ ) {
				if ( m == n ) {
					continue;
				}
				for( int k=0; k < cardinality[ n ]; k++ ) {
					j++;
				}
			}
		}
		System.out.println( "" + j );
	}
	*/
}
