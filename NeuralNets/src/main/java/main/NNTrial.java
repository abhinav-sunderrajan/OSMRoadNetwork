package main;

import java.util.Random;

import network.NeuralNetwork;
import network.NeuralNetwork.REGULARIZATION;

import org.la4j.Vector;
import org.la4j.vector.DenseVector;

import utils.TrainingData;
import costfunction.CrossEntropyCost;

/**
 * 
 * @author abhinav.sunderrajan
 * 
 */
public class NNTrial {

	public static void main(String args[]) {
		int[] layers = { 1, 8, 1 };

		Random random = new Random();
		TrainingData td = new TrainingData();

		for (int i = 0; i < 100; i++) {
			double x = random.nextDouble();

			double[] ip = { x };
			Vector input = DenseVector.fromArray(ip);
			double op[] = { fX(x) };
			Vector output = DenseVector.fromArray(op);
			td.addTrainingData(input, output);
		}
		NeuralNetwork nn = NeuralNetwork.getNNInstance(random.nextLong(), layers, 1,
				new CrossEntropyCost());
		nn.setRegularization(REGULARIZATION.L2);
		nn.setTd(td);

		nn.stochasticGradientDescent(td, 10, 300, 0.1);
		double[] ip = { 0.8 };
		System.out.println("nnop:" + nn.feedForward(DenseVector.fromArray(ip)) + " real op:"
				+ fX(0.8));
	}

	private static double fX(double x) {
		return 0.2 + 0.4 * x * x + 0.3 * x * Math.sin(15.0 * x) + 0.05 * Math.cos(50.0 * x);
	}

}
