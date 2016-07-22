package network;

import org.la4j.Matrix;
import org.la4j.Vector;

import activations.Activation;
import activations.Sigmoid;

/**
 * 
 * @author abhinav.sunderrajan
 * 
 */
public class Layer {

	private Matrix weight;
	private Vector bias;
	private Activation activation;

	public Layer() {
		this.activation = new Sigmoid();
	}

	/**
	 * A layer in the neural network.
	 * 
	 * @param weight
	 * @param bias
	 * @param activation
	 */
	public Layer(Matrix weight, Vector bias, Activation activation) {
		this.weight = weight;
		this.bias = bias;
		this.activation = activation;
	}

	/**
	 * returns the output for this layer of the NN depending upon the activation
	 * function chosen.
	 * 
	 * @param input
	 * @return
	 */
	public Vector getLayerOutput(Vector input) {
		Vector output = activation.sigma(weight.multiply(input).add(bias));
		return output;
	}

	/**
	 * @return the weight
	 */
	public Matrix getWeight() {
		return weight;
	}

	/**
	 * @param weight
	 *            the weight to set
	 */
	public void setWeight(Matrix weight) {
		this.weight = weight;
	}

	/**
	 * @return the bias
	 */
	public Vector getBias() {
		return bias;
	}

	/**
	 * @param bias
	 *            the bias to set
	 */
	public void setBias(Vector bias) {
		this.bias = bias;
	}

	/**
	 * @return the activation
	 */
	public Activation getActivationFunction() {
		return activation;
	}

	/**
	 * @param activation
	 *            the activation to set
	 */
	public void setActivation(Activation activation) {
		this.activation = activation;
	}

}
