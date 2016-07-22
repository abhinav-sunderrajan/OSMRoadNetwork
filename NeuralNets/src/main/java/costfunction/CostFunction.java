package costfunction;

import java.text.DecimalFormat;

import org.la4j.Vector;

import activations.Activation;

public interface CostFunction {

	public static final DecimalFormat df = new DecimalFormat("#.####");

	/**
	 * Returns the error in the Neural Network based on the error between the
	 * output layer of the NN and the desired output.
	 * 
	 * @param desiredOutput
	 * @param nnOutput
	 * @return
	 */
	public abstract double getCost(Vector desiredOutput, Vector nnOutput)
			throws IllegalArgumentException;

	/**
	 * delta_C/delta_a
	 * 
	 * @param outputActivations
	 * @param output
	 * @param z
	 * @param activation
	 * @return
	 */
	public abstract Vector delta(Vector outputActivations, Vector output, Vector z,
			Activation activation);

}
