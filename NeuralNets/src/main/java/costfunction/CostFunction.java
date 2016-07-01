package costfunction;

import org.la4j.Vector;

public interface CostFunction {

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
	 * @return
	 */
	public abstract Vector delta(Vector outputActivations, Vector output, Vector z);

}