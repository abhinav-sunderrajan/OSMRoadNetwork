package activations;

import org.la4j.Vector;

/**
 * Activation for a neuron.
 * 
 * @author abhinav.sunderrajan
 * 
 */
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

	/**
	 * Using it for gradient based reinforcement learning refer {@link http
	 * ://incompleteideas.net/sutton/williams-92.pdf}
	 * 
	 * @param z
	 * @return
	 */
	public Vector sigmaPrimeBySigma(Vector z);

}
