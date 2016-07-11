package costfunction;

import org.la4j.Vector;

import utils.CommonUtils;

public class QuadraticCost implements CostFunction {

	@Override
	public double getCost(Vector desiredOutput, Vector nnOutput) {
		double cost = 0.0;
		if (desiredOutput.length() != nnOutput.length())
			throw new IllegalArgumentException(
					"The length of the desired output and NN output cannot be different");
		for (int i = 0; i < desiredOutput.length(); i++) {
			cost += (nnOutput.get(i) - desiredOutput.get(i))
					* (nnOutput.get(i) - desiredOutput.get(i));
		}
		return Double.parseDouble(df.format(cost / 2.0));
	}

	@Override
	public Vector delta(Vector outputActivations, Vector output, Vector z) {
		Vector derivative = outputActivations.subtract(output);
		return derivative.hadamardProduct(CommonUtils.sigmoidPrime(z));
	}
}
