package activations;

import org.la4j.Vector;
import org.la4j.vector.functor.VectorFunction;

/**
 * Leaky rectified linear units
 * 
 * @author abhinav.sunderrajan
 * 
 */
public class ReLU implements Activation {

	@Override
	public Vector sigma(Vector z) {
		return z.transform(new VectorFunction() {

			@Override
			public double evaluate(int i, double value) {
				double ret = value > 0.0 ? value : 0.01 * value;
				return ret;
			}
		});
	}

	@Override
	public Vector sigmaPrime(Vector z) {
		return z.transform(new VectorFunction() {

			@Override
			public double evaluate(int i, double value) {
				double ret = value > 0.0 ? 1.0 : 0.01;
				return ret;
			}
		});
	}

	@Override
	public Vector sigmaPrimeBySigma(Vector z) {
		return z.transform(new VectorFunction() {

			@Override
			public double evaluate(int i, double value) {
				if (value == 0.0)
					return value;
				else {
					return 1 / value;
				}
			}
		});
	}

}
