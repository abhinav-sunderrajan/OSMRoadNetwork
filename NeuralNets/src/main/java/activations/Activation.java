package activations;

import org.la4j.Vector;

public interface Activation {

	/**
	 * The activation function for a neural network layer.
	 * 
	 * @param z
	 * @return
	 */
	public Vector sigma(Vector z);

	/**
	 * The derivative of the activation function.
	 * 
	 * @param z
	 * @return
	 */
	public Vector sigmaPrime(Vector z);

}
