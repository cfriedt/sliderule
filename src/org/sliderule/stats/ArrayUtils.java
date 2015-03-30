package org.sliderule.stats;

public final class ArrayUtils {
	private ArrayUtils() {}
	
	public static <T extends Number> Object toPrimitive( T[] a ) {
		return toPrimitive( a );
	}
	public static byte[] toPrimitive( Byte[] a ) {
		byte[] r = null;
		if ( null != a ) {
			r = new byte[ a.length ];
			for( int i = 0; i < a.length; i++ ) {
				r[ i ] = (byte) a[ i ];
			}
		}
		return r;
	}
	public static char[] toPrimitive( Character[] a ) {
		char[] r = null;
		if ( null != a ) {
			r = new char[ a.length ];
			for( int i = 0; i < a.length; i++ ) {
				r[ i ] = (char) a[ i ];
			}
		}
		return r;
	}
	public static short[] toPrimitive( Short[] a ) {
		short[] r = null;
		if ( null != a ) {
			r = new short[ a.length ];
			for( int i = 0; i < a.length; i++ ) {
				r[ i ] = (short) a[ i ];
			}
		}
		return r;
	}
	public static int[] toPrimitive( Integer[] a ) {
		int[] r = null;
		if ( null != a ) {
			r = new int[ a.length ];
			for( int i = 0; i < a.length; i++ ) {
				r[ i ] = (int) a[ i ];
			}
		}
		return r;
	}
	public static long[] toPrimitive( Long[] a ) {
		long[] r = null;
		if ( null != a ) {
			r = new long[ a.length ];
			for( int i = 0; i < a.length; i++ ) {
				r[ i ] = (long) a[ i ];
			}
		}
		return r;
	}
	public static float[] toPrimitive( Float[] a ) {
		float[] r = null;
		if ( null != a ) {
			r = new float[ a.length ];
			for( int i = 0; i < a.length; i++ ) {
				r[ i ] = (float) a[ i ];
			}
		}
		return r;
	}
	public static double[] toPrimitive( Double[] a ) {
		double[] r = null;
		if ( null != a ) {
			r = new double[ a.length ];
			for( int i = 0; i < a.length; i++ ) {
				r[ i ] = (double) a[ i ];
			}
		}
		return r;
	}
}
