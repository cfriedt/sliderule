package examples;

abstract class AOperation {
	protected static int checkValidNumber( int number ) {
		if ( number < 0 ) {
			throw new IllegalArgumentException();
		}
		return number;
	}
	abstract long iterative( int reps );
	abstract long recursive( int reps );
}
