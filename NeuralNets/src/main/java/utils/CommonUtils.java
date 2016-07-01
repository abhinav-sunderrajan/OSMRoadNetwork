package utils;

import org.la4j.Matrix;
import org.la4j.Vector;
import org.la4j.matrix.functor.MatrixFunction;
import org.la4j.vector.functor.VectorFunction;

public class CommonUtils {

	/**
	 * Implementation of the sigmoid function on vector. Note that it is an
	 * element wise operation.
	 * 
	 * @param z
	 * @return
	 */
	public static Vector sigmoid(Vector z) {
		return z.transform(new VectorFunction() {

			@Override
			public double evaluate(int i, double value) {
				return 1.0 / (1.0 + Math.exp(-value));
			}
		});
	}

	/**
	 * Implementation of the differential of sigmoid function. It is an element
	 * wise operation.
	 * 
	 * @param z
	 * @return
	 */
	public static Vector sigmoidPrime(Vector z) {
		return z.transform(new VectorFunction() {

			@Override
			public double evaluate(int i, double value) {
				double sigma = 1.0 / (1.0 + Math.exp(-value));
				return sigma * (1.0 - sigma);
			}
		});
	}

	/**
	 * Implementation of the RELU function on vector. Note that it is an element
	 * wise operation.
	 * 
	 * @param z
	 * @return
	 */
	public static Vector reLU(Vector z) {
		return z.transform(new VectorFunction() {

			@Override
			public double evaluate(int i, double value) {
				double ret = value > 0.0 ? value : 0.01 * value;
				return ret;
			}
		});
	}

	/**
	 * Implementation of the RELU function on vector. Note that it is an element
	 * wise operation.
	 * 
	 * @param z
	 * @return
	 */
	public static Vector reLUPrime(Vector z) {
		return z.transform(new VectorFunction() {

			@Override
			public double evaluate(int i, double value) {
				double ret = value > 0.0 ? 1.0 : 0.01;
				return ret;
			}
		});
	}

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
