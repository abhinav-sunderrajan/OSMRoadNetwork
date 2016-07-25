package main;

import org.la4j.Vector;
import org.la4j.vector.DenseVector;
import org.la4j.vector.functor.VectorFunction;

public class TBD {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		double a[] = { 1, 2, 3 };
		Vector v = DenseVector.fromArray(a);
		System.out.println(sigma(v));
		System.out.println(sigmaPrime(v));
	}

	public static Vector sigma(Vector z) {
		return z.transform(new VectorFunction() {

			@Override
			public double evaluate(int i, double value) {
				return 1.0 / (1.0 + Math.exp(-value));
			}
		});
	}

	public static Vector sigmaPrime(Vector z) {
		return z.transform(new VectorFunction() {

			@Override
			public double evaluate(int i, double value) {
				double sigma = 1.0 / (1.0 + Math.exp(-value));
				return sigma * (1.0 - sigma);
			}
		});
	}

}
