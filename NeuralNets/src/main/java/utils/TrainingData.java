package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.la4j.Vector;

/**
 * Training data is a list of tuples "(x, y)" representing the training inputs
 * and the desired outputs.
 * 
 * @author abhinav.sunderrajan
 * 
 */
public class TrainingData {

	private List<Vector> inputs;
	private List<Vector> outputs;
	private static Random random = new Random();

	public TrainingData() {
		this.inputs = new ArrayList<Vector>();
		this.outputs = new ArrayList<Vector>();
	}

	/**
	 * Initialize the training data blob.
	 * 
	 * @param inputs
	 * @param outputs
	 */
	public TrainingData(List<Vector> inputs, List<Vector> outputs) {
		if (inputs.size() != outputs.size()) {
			throw new IllegalArgumentException(
					"The number of inputs must be equal to the number of outputs");
		}
		this.inputs = inputs;
		this.outputs = outputs;
	}

	/**
	 * Add training data to the blob.
	 * 
	 * @param input
	 * @param output
	 */
	public void addTrainingData(Vector input, Vector output) {
		if (inputs.size() > 0 && input.length() != inputs.get(0).length())
			throw new IllegalArgumentException("The size of the input vector is not consistant");

		if (outputs.size() > 0 && output.length() != outputs.get(0).length())
			throw new IllegalArgumentException("The size of the output vector is not consistant");

		inputs.add(input);
		outputs.add(output);
	}

	/**
	 * @return the inputs
	 */
	public List<Vector> getInputs() {
		return inputs;
	}

	/**
	 * @return the outputs
	 */
	public List<Vector> getOutputs() {
		return outputs;
	}

	/**
	 * Returns a batch from
	 * 
	 * @param batchSize
	 * @return
	 */
	public TrainingData miniBatch(int batchSize) {
		batchSize = batchSize > inputs.size() ? inputs.size() : batchSize;
		TrainingData batch = new TrainingData();
		List<Integer> temp = new ArrayList<>();
		while (temp.size() < batchSize) {
			int index = random.nextInt(this.inputs.size());
			if (!temp.contains(index)) {
				batch.getInputs().add(this.inputs.get(index));
				batch.getOutputs().add(this.outputs.get(index));
				temp.add(index);
			}
		}
		return batch;

	}

	/**
	 * Divide the original training data into training and validation sets.
	 * 
	 * @param percentage
	 *            of the original data that goes into the training set.
	 * @return the training and validation set array.
	 */
	public TrainingData[] trainingAndValidationSet(double percentage) {
		percentage = percentage >= 1.0 ? 0.9 : percentage;
		TrainingData training = new TrainingData();
		TrainingData validation = new TrainingData();

		for (int i = 0; i < inputs.size(); i++) {
			if (random.nextDouble() < percentage) {
				training.getInputs().add(this.inputs.get(i));
				training.getOutputs().add(this.outputs.get(i));
			} else {
				validation.getInputs().add(this.inputs.get(i));
				validation.getOutputs().add(this.outputs.get(i));
			}

		}

		TrainingData[] trainAndValidate = { training, validation };
		return trainAndValidate;

	}
}
