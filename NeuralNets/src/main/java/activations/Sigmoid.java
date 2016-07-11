package activations;

import org.la4j.Vector;
import org.la4j.vector.functor.VectorFunction;

public class Sigmoid implements Activation {

	@Override
	public Vector sigma(Vector z) {
		return z.transform(new VectorFunction() {

			@Override
			public double evaluate(int i, double value) {
				return 1.0 / (1.0 + Math.exp(-value));
			}
		});
	}

	@Override
	public Vector sigmaPrime(Vector z) {
		return z.transform(new VectorFunction() {

			@Override
			public double evaluate(int i, double value) {
				double sigma = 1.0 / (1.0 + Math.exp(-value));
				return sigma * (1.0 - sigma);
			}
		});
	}

}
