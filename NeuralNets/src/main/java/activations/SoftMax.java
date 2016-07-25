package activations;

import org.la4j.Vector;
import org.la4j.vector.functor.VectorFunction;

/**
 * Softmax is always used for the output layer.
 * 
 * @author abhinav.sunderrajan
 * 
 */
public class SoftMax implements Activation {

	@Override
	public Vector sigma(Vector z) {
		double sum = 0.0;
		for (double d : z)
			sum += Math.exp(d);

		final double t = sum;
		return z.transform(new VectorFunction() {

			@Override
			public double evaluate(int i, double value) {
				return Math.exp(value) / t;
			}
		});
	}

	@Override
	/**
	 * Since we always use cross
	 * entropy with softmax we do not need the derivative.
	 */
	public Vector sigmaPrime(Vector z) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector sigmaPrimeBySigma(Vector z) {
		// TODO Auto-generated method stub
		return null;
	}

}
