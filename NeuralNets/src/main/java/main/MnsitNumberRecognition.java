package main;

import java.util.Random;

import network.NeuralNetwork;
import network.NeuralNetwork.REGULARIZATION;
import utils.TrainingData;
import costfunction.CrossEntropyCost;

public class MnsitNumberRecognition {

	private static final String labelFilename = "C:\\Users\\abhinav.sunderrajan\\Downloads\\train-labels.idx1-ubyte";
	private static final String imageFilename = "C:\\Users\\abhinav.sunderrajan\\Downloads\\train-images.idx3-ubyte";

	public static void main(String args[]) {
		TrainingData allData = MNSITDataLoader.getMNSITTrainingData(labelFilename, imageFilename);
		TrainingData smallPart = allData.miniBatch(12000);
		TrainingData[] data = smallPart.trainingAndValidationSet(0.8);
		System.out.println("Loaded training data..");
		int[] layers = { 784, 100, 10 };
		Random random = new Random();
		NeuralNetwork nn = NeuralNetwork.getNNInstance(random.nextLong(), layers,
				new CrossEntropyCost());
		nn.setTrainingData(data[0]);
		nn.setValidationData(data[1]);

		nn.setRegularization(REGULARIZATION.L2);
		nn.stochasticGradientDescent(10, 80, 0.3);

	}

}
