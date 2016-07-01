package network;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.la4j.Matrix;
import org.la4j.Vector;
import org.la4j.matrix.DenseMatrix;
import org.la4j.vector.DenseVector;

import utils.CommonUtils;
import utils.TrainingData;
import costfunction.CostFunction;

/**
 * A Neural network object.
 * 
 * @author abhinav.sunderrajan
 * 
 */
public class NeuralNetwork {
	private int numOfLayers;
	private Vector[] bias;
	private Matrix[] weights;
	private Random random;
	private TrainingData td;
	private CostFunction costFunction;
	private double lambda = 10.0;
	private static NeuralNetwork network;

	public static enum REGULARIZATION {
		L1, L2
	};

	private REGULARIZATION regularization;

	/**
	 * Returns an instance of the Neural Network. Specify the the number of
	 * neurons in each layer.
	 * 
	 * @param seed
	 *            the seed to initialize the random component.
	 * @param layers
	 *            the vararg represents the number of neurons in each layer.
	 *            Does not include the inputs
	 * @param numOfInputs
	 *            number of inputs to the neural network.
	 * @return
	 */
	public static NeuralNetwork getNNInstance(long seed, int[] layers, int numOfInputs,
			CostFunction costfunction) {
		if (network == null) {
			network = new NeuralNetwork(seed, layers, numOfInputs, costfunction);
		}
		return network;
	}

	/**
	 * The integer vector determines the number of neurons in each layer.
	 * 
	 * @param layers
	 * @param costfunction
	 * @param nnInput
	 */
	private NeuralNetwork(long seed, int[] layers, int numOfInputs, CostFunction costfunction) {
		this.random = new Random(seed);
		this.costFunction = costfunction;
		numOfLayers = layers.length;
		bias = new Vector[numOfLayers];
		weights = new Matrix[numOfLayers];
		regularization = REGULARIZATION.L1;

		for (int index = 0; index < layers.length; index++) {
			double arr[] = new double[layers[index]];
			for (int i = 0; i < arr.length; i++)
				arr[i] = random.nextGaussian() * 1.0 / 8.0;
			bias[index] = DenseVector.fromArray(arr);
		}

		for (int index = 0; index < layers.length; index++) {
			int nRow = layers[index];
			int nCol = numOfInputs;
			if (index > 0)
				nCol = layers[index - 1];
			double arr[][] = new double[nRow][nCol];
			for (int i = 0; i < nRow; i++) {
				for (int j = 0; j < nCol; j++)
					arr[i][j] = random.nextGaussian() * 1.0 / 8.0;
			}

			weights[index] = DenseMatrix.from2DArray(arr);
		}

	}

	/**
	 * @return the regularization
	 */
	public REGULARIZATION getRegularization() {
		return regularization;
	}

	/**
	 * @param regularization
	 *            the regularization to set
	 */
	public void setRegularization(REGULARIZATION regularization) {
		this.regularization = regularization;
	}

	/**
	 * @return the td
	 */
	public TrainingData getTd() {
		return td;
	}

	/**
	 * @param td
	 *            the td to set
	 */
	public void setTd(TrainingData td) {
		this.td = td;
	}

	/**
	 * @return the numOfLayers
	 */
	public int getNumOfLayers() {
		return numOfLayers;
	}

	/**
	 * @return the bias
	 */
	public Vector[] getBias() {
		return bias;
	}

	/**
	 * @return the weights
	 */
	public Matrix[] getWeights() {
		return weights;
	}

	/**
	 * Return the out put of the configured neural network.
	 * 
	 * @param input
	 * @return
	 */
	public Vector feedForward(Vector input) {
		for (int i = 0; i < bias.length; i++) {
			input = CommonUtils.sigmoid(weights[i].multiply(input).add(bias[i]));
		}
		return input;

	}

	/**
	 * Returns the error associated with the NN based on the training data
	 * provided.
	 * 
	 * @return
	 */
	public double getError() {
		double cost = 0.0;
		int n = td.getInputs().size();
		for (int input = 0; input < n; input++) {
			Vector nnOutput = feedForward(td.getInputs().get(input));
			Vector desiredOutput = td.getOutputs().get(input);
			for (int i = 0; i < desiredOutput.length(); i++) {
				cost += costFunction.getCost(desiredOutput, nnOutput);
			}
		}

		return cost / td.getInputs().size();

	}

	/**
	 * Implementation of stochastic gradient descent.
	 * 
	 * @param trainingData
	 *            the training data.
	 * @param batchSize
	 *            batch size
	 * @param epochs
	 *            number of epochs to train for
	 * @param eta
	 *            the learning rate.
	 */
	public void stochasticGradientDescent(TrainingData trainingData, int batchSize, int epochs,
			double eta) {
		int n = trainingData.getInputs().size();

		for (int epoch = 0; epoch < epochs; epoch++) {
			TrainingData batch = trainingData.miniBatch(batchSize);
			for (int i = 0; i < batchSize; i++) {
				WeighBiasUpdate update = backPropagation(batch.getInputs().get(i), batch
						.getOutputs().get(i));
				for (int index = 0; index < update.biasUpdate.size(); index++) {
					switch (regularization) {
					case L1:
						weights[index].subtract(
								CommonUtils.signum(weights[index]).multiply(eta * lambda / n))
								.subtract(update.weightUpdate.get(index).multiply(eta / batchSize));
						break;
					default:
						weights[index] = weights[index].multiply((1 - eta * (lambda / n)))
								.subtract(update.weightUpdate.get(index).multiply(eta / batchSize));
						break;
					}
					bias[index] = bias[index].subtract(update.biasUpdate.get(index).multiply(
							eta / batchSize));
				}

			}

			System.out.println("Epoch " + (epoch + 1) + " error\t" + getError());
		}

	}

	private WeighBiasUpdate backPropagation(Vector input, Vector output) {
		List<Vector> activations = new ArrayList<>();
		List<Vector> zlList = new ArrayList<>();

		List<Vector> costBiasDer = new ArrayList<>();
		List<Matrix> costWeightDer = new ArrayList<>();
		Vector activation = input;
		activations.add(activation);
		int index = 0;
		while (index < numOfLayers) {
			Vector zl = weights[index].multiply(activation).add(bias[index]);
			zlList.add(zl);
			activation = CommonUtils.sigmoid(zl);
			activations.add(activation);
			index++;
		}

		Vector delta = costFunction.delta(activations.get(numOfLayers), output,
				zlList.get(numOfLayers - 1));

		costBiasDer.add(delta);
		costWeightDer.add(delta.outerProduct(activations.get(numOfLayers - 1)));

		for (int layer = numOfLayers - 2; layer >= 0; layer--) {
			Vector z = zlList.get(layer);
			Vector sp = CommonUtils.sigmoidPrime(z);
			delta = weights[layer + 1].transpose().multiply(delta).hadamardProduct(sp);
			costBiasDer.add(0, delta);
			costWeightDer.add(0, delta.outerProduct(activations.get(layer)));
		}

		return new WeighBiasUpdate(costWeightDer, costBiasDer);

	}

	private class WeighBiasUpdate {
		List<Matrix> weightUpdate;
		List<Vector> biasUpdate;

		/**
		 * @param weightUpdate
		 * @param biasUpdate
		 */
		public WeighBiasUpdate(List<Matrix> weightUpdate, List<Vector> biasUpdate) {
			this.weightUpdate = weightUpdate;
			this.biasUpdate = biasUpdate;
		}

	}

	/**
	 * @return the regularizationParam
	 */
	public double getRegularizationParam() {
		return lambda;
	}

	/**
	 * @param regularizationParam
	 *            the regularizationParam to set
	 */
	public void setRegularizationParam(double regularizationParam) {
		this.lambda = regularizationParam;
	}

}
