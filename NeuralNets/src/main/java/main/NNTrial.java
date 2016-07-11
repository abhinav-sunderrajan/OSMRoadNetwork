package main;

import java.util.Random;

import network.NeuralNetwork;
import network.NeuralNetwork.REGULARIZATION;

import org.la4j.Vector;
import org.la4j.vector.DenseVector;

import utils.TrainingData;
import costfunction.QuadraticCost;

/**
 * 
 * @author abhinav.sunderrajan
 * 
 */
public class NNTrial {

	public static void main(String args[]) {
		int[] layers = { 2, 3, 1 };

		Random random = new Random();
		TrainingData td = new TrainingData();

		for (int i = 0; i < 100; i++) {
			double x = 100.0 * random.nextDouble();
			double y = 100.0 * random.nextDouble();

			double[] ip = { x, y };
			Vector input = DenseVector.fromArray(ip);
			double op[] = { fX(ip) };
			Vector output = DenseVector.fromArray(op);
			td.addTrainingData(input, output);
		}
		NeuralNetwork nn = NeuralNetwork.getNNInstance(random.nextLong(), layers, 2,
				new QuadraticCost());
		nn.setRegularization(REGULARIZATION.L2);
		nn.setLambda(0.0);
		nn.setTd(td);
		nn.stochasticGradientDescent(td, 20, 600, 0.1);
		double[] ip = { 80.0, 90.0 };
		System.out.println("nnop:" + nn.feedForward(DenseVector.fromArray(ip)) + " real op:"
				+ fX(ip));
	}

	private static double fX(double[] x) {
		return 0.2 + 0.4 * x[0] + 0.3 * x[1];
	}

}
