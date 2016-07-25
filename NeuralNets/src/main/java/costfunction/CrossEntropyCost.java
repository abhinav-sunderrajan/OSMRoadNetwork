package costfunction;

import network.NeuralNetwork.REGULARIZATION;

import org.la4j.Matrix;
import org.la4j.Vector;
import org.la4j.matrix.functor.MatrixFunction;

import activations.Activation;

public class CrossEntropyCost implements CostFunction {

	private REGULARIZATION r;
	private Matrix[] weights;
	private int n;
	private double lambda;
	private double matSum;

	/**
	 * Cross entropy cost
	 * 
	 * @param regularization
	 * @param weights
	 *            neural network weights
	 * @param n
	 *            training data size
	 * @param lambda
	 *            the regularization parameter.
	 */
	public CrossEntropyCost(REGULARIZATION regularization, Matrix[] weights, int n, double lambda) {
		this.r = regularization;
		this.weights = weights;
		this.n = n;
		this.lambda = lambda;
		matSum = -1.0;
	}

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

		// if (matSum == -1.0) {
		// switch (r) {
		// case L1:
		// for (Matrix weight : weights) {
		// weight = absMatrix(weight);
		// matSum += weight.sum();
		// }
		// matSum = lambda * matSum / (n);
		// break;
		//
		// case L2:
		// for (Matrix weight : weights) {
		// double s = weight.sum();
		// matSum += s * s;
		// }
		// matSum = lambda * matSum / (2.0 * n);
		// break;
		// default:
		// matSum = 0.0;
		// break;
		// }
		// }
		//
		// cost += matSum;
		return Double.parseDouble(df.format(cost));
	}

	/**
	 * @return the matSum
	 */
	public double getMatSum() {
		return matSum;
	}

	/**
	 * @param matSum
	 *            the matSum to set
	 */
	public void setMatSum(double matSum) {
		this.matSum = matSum;
	}

	@Override
	public Vector delta(Vector outputActivations, Vector output, Vector z, Activation activation) {
		Vector delta = outputActivations.subtract(output);
		return delta;
	}

	private Matrix absMatrix(Matrix m) {
		return m.transform(new MatrixFunction() {

			@Override
			public double evaluate(int i, int j, double value) {
				return Math.abs(value);
			}
		});
	}
}
