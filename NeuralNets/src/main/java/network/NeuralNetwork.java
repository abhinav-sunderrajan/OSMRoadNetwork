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
import costfunction.QuadraticCost;

/**
 * A Neural network object.
 * 
 * @author abhinav.sunderrajan
 * 
 */
public class NeuralNetwork {
	private int numOfLayers;
	private Random random;
	private TrainingData trainingData;
	private TrainingData validationData;
	private CostFunction costFunction;
	private double lambda = 1.0;
	private Layer[] nnLayers;
	private static NeuralNetwork network;

	public static enum REGULARIZATION {
		L1, L2, NONE
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
	 * @return
	 */
	public static NeuralNetwork getNNInstance(long seed, int[] layers) {
		if (network == null) {
			network = new NeuralNetwork(seed, layers);
		}
		return network;
	}

	/**
	 * The integer vector determines the number of neurons in each layer.
	 * 
	 * @param numNeurons
	 * @param costfunction
	 * @param nnInput
	 */
	private NeuralNetwork(long seed, int[] numNeurons) {
		this.random = new Random(seed);
		this.costFunction = new QuadraticCost();
		numOfLayers = numNeurons.length;
		nnLayers = new Layer[numOfLayers];
		regularization = REGULARIZATION.NONE;

		for (int index = 0; index < numNeurons.length; index++) {
			nnLayers[index] = new Layer();
			Vector bias = DenseVector.zero(numNeurons[index]);
			if (index > 0) {
				for (int i = 0; i < bias.length(); i++)
					bias.set(i, random.nextGaussian());
			}
			nnLayers[index].setBias(bias);
		}

		for (int index = 0; index < numNeurons.length; index++) {
			int nRow = numNeurons[index];
			int nCol = numNeurons[0];
			if (index > 0)
				nCol = numNeurons[index - 1];
			double arr[][] = new double[nRow][nCol];
			int div = nCol > 100 ? 100 : nCol;
			for (int i = 0; i < nRow; i++) {
				for (int j = 0; j < nCol; j++)
					arr[i][j] = random.nextGaussian() / Math.sqrt(nCol);
			}

			nnLayers[index].setWeight(DenseMatrix.from2DArray(arr));
		}

	}

	/**
	 * @return the costFunction
	 */
	public CostFunction getCostFunction() {
		return costFunction;
	}

	/**
	 * @param costFunction
	 *            the costFunction to set
	 */
	public void setCostFunction(CostFunction costFunction) {
		this.costFunction = costFunction;
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
		return trainingData;
	}

	/**
	 * @param td
	 *            the td to set
	 */
	public void setTrainingData(TrainingData td) {
		this.trainingData = td;
	}

	/**
	 * @return the numOfLayers
	 */
	public int getNumOfLayers() {
		return numOfLayers;
	}

	/**
	 * @return the validationData
	 */
	public TrainingData getValidationData() {
		return validationData;
	}

	/**
	 * @param validationData
	 *            the validationData to set
	 */
	public void setValidationData(TrainingData validationData) {
		this.validationData = validationData;
	}

	/**
	 * Return the output of the configured neural network.
	 * 
	 * @param input
	 * @return
	 */
	public Vector feedForward(Vector input) {
		for (int i = 0; i < nnLayers.length; i++)
			input = nnLayers[i].getLayerOutput(input);

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
		int n = trainingData.getInputs().size();
		for (int input = 0; input < n; input++) {
			Vector nnOutput = feedForward(trainingData.getInputs().get(input));
			Vector desiredOutput = trainingData.getOutputs().get(input);
			cost += costFunction.getCost(desiredOutput, nnOutput);
		}
		return cost / trainingData.getInputs().size();

	}

	public int evaluate() {
		int vdSize = validationData.getInputs().size();
		int correct = 0;
		for (int input = 0; input < vdSize; input++) {
			Vector nnOutput = feedForward(validationData.getInputs().get(input));
			Vector desiredOutput = validationData.getOutputs().get(input);
			int maxIndex1 = 0;
			int maxIndex2 = 0;
			double max = nnOutput.max();
			for (int i = 0; i < nnOutput.length(); i++) {
				if (nnOutput.get(i) == max) {
					maxIndex1 = i;
					break;
				}
			}
			max = desiredOutput.max();
			for (int i = 0; i < desiredOutput.length(); i++) {
				if (desiredOutput.get(i) == max) {
					maxIndex2 = i;
					break;
				}
			}

			if (maxIndex1 == maxIndex2)
				correct++;

		}

		return correct;
	}

	/**
	 * Implementation of stochastic gradient descent.
	 * 
	 * @param batchSize
	 *            batch size
	 * @param epochs
	 *            number of epochs to train for
	 * @param eta
	 *            the learning rate.
	 */
	public void stochasticGradientDescent(int batchSize, int epochs, double eta) {
		int n = trainingData.getInputs().size();
		double error = getError();
		int epoch = 0;
		do {
			TrainingData batch = trainingData.miniBatch(batchSize);
			for (int i = 0; i < batchSize; i++) {
				WeighBiasUpdate update = backPropagation(batch.getInputs().get(i), batch
						.getOutputs().get(i));
				for (int layer = 0; layer < update.biasUpdate.size(); layer++) {
					Matrix weight = nnLayers[layer].getWeight();
					Vector bias = nnLayers[layer].getBias();
					switch (regularization) {
					case L1:
						weight = weight.subtract(
								CommonUtils.signum(nnLayers[layer].getWeight()).multiply(
										eta * lambda / n)).subtract(
								update.weightUpdate.get(layer).multiply(eta / batchSize));
						break;
					case L2:
						double scale = 1.0 - eta * (lambda / n);
						weight = weight.multiply(scale);
						weight = weight.subtract(update.weightUpdate.get(layer).multiply(
								eta / batchSize));
						break;
					default:
						weight = weight.subtract(update.weightUpdate.get(layer).multiply(
								eta / batchSize));
					}
					if (layer > 0)
						bias = bias
								.subtract(update.biasUpdate.get(layer).multiply(eta / batchSize));
					nnLayers[layer].setBias(bias);
					nnLayers[layer].setWeight(weight);
				}

			}
			error = getError();
			System.out.println((epoch + 1) + "\t" + error);
			epoch++;
		} while (error > 0.01 && epoch < 5000);

		// System.out.println("Accuracy:" + evaluate());

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
			Vector zl = nnLayers[index].getWeight().multiply(activation)
					.add(nnLayers[index].getBias());
			zlList.add(zl);
			activation = nnLayers[index].getLayerOutput(activation);
			activations.add(activation);
			index++;
		}

		Vector delta = costFunction.delta(activations.get(numOfLayers), output,
				zlList.get(numOfLayers - 1), nnLayers[numOfLayers - 1].getActivationFunction());

		costBiasDer.add(delta);
		costWeightDer.add(delta.outerProduct(activations.get(numOfLayers - 1)));

		for (int layer = numOfLayers - 2; layer >= 0; layer--) {
			Vector z = zlList.get(layer);
			Vector sp = nnLayers[layer].getActivationFunction().sigmaPrime(z);
			delta = nnLayers[layer + 1].getWeight().transpose().multiply(delta).hadamardProduct(sp);
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

	/**
	 * @return the lambda
	 */
	public double getLambda() {
		return lambda;
	}

	/**
	 * @param lambda
	 *            the lambda to set
	 */
	public void setLambda(double lambda) {
		this.lambda = lambda;
	}

	/**
	 * @return the nnLayers
	 */
	public Layer[] getNnLayers() {
		return nnLayers;
	}

}
