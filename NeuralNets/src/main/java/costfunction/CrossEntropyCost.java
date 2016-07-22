package costfunction;

import org.la4j.Vector;

import activations.Activation;

public class CrossEntropyCost implements CostFunction {

	@Override
	public double getCost(Vector desiredOutput, Vector nnOutput) throws IllegalArgumentException {
		double cost = 0.0;

		for (int i = 0; i < desiredOutput.length(); i++) {
			double temp = -(desiredOutput.get(i) * Math.log(nnOutput.get(i)) + (1.0 - desiredOutput
					.get(i)) * Math.log(1.0 - nnOutput.get(i)));

			if (Double.isNaN(temp))
				temp = 0.0;
			cost += temp;
		}

		return Double.parseDouble(df.format(cost));
	}

	@Override
	public Vector delta(Vector outputActivations, Vector output, Vector z, Activation activation) {
		Vector delta = outputActivations.subtract(output);
		return delta;
	}
}
