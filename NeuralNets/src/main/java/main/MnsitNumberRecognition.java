package main;

import java.util.Random;

import network.NeuralNetwork;
import network.NeuralNetwork.REGULARIZATION;

import org.la4j.Matrix;

import utils.TrainingData;
import costfunction.CrossEntropyCost;

public class MnsitNumberRecognition {

	private static final String labelFilename = "C:\\Users\\abhinav.sunderrajan\\Downloads\\train-labels.idx1-ubyte";
	private static final String imageFilename = "C:\\Users\\abhinav.sunderrajan\\Downloads\\train-images.idx3-ubyte";

	public static void main(String args[]) {
		TrainingData allData = MNSITDataLoader.getMNSITTrainingData(labelFilename, imageFilename);
		TrainingData smallPart = allData.miniBatch(2000);
		TrainingData[] data = smallPart.trainingAndValidationSet(0.8);
		System.out.println("Loaded training data..");
		int[] layers = { 784, 30, 10 };
		Random random = new Random();
		NeuralNetwork nn = NeuralNetwork.getNNInstance(random.nextLong(), layers);
		nn.setLambda(5.0);
		nn.setTrainingData(data[0]);
		nn.setValidationData(data[1]);
		Matrix weights[] = new Matrix[nn.getNumOfLayers()];
		for (int i = 0; i < weights.length; i++)
			weights[i] = nn.getNnLayers()[i].getWeight();

		nn.setCostFunction(new CrossEntropyCost(REGULARIZATION.L2, weights, nn.getTd().getInputs()
				.size(), 5.0));

		nn.stochasticGradientDescent(10, 200, 4.5);

	}
}
