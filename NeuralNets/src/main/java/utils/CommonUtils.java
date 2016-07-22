package utils;

import org.la4j.Matrix;
import org.la4j.matrix.functor.MatrixFunction;

public class CommonUtils {

	/**
	 * Implementation of signum function over all elements of the matrix
	 * 
	 * @param matrix
	 * @return
	 */
	public static Matrix signum(Matrix matrix) {
		return matrix.transform(new MatrixFunction() {
			@Override
			public double evaluate(int i, int j, double value) {
				return value > 0 ? 1.0 : 0.0;
			}
		});

	}
}
