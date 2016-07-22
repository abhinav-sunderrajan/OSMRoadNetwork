package main;

import java.util.Random;

import network.Layer;
import network.NeuralNetwork;
import network.NeuralNetwork.REGULARIZATION;

import org.la4j.Vector;
import org.la4j.vector.DenseVector;

import utils.TrainingData;
import activations.ReLU;
import costfunction.QuadraticCost;

/**
 * 
 * @author abhinav.sunderrajan
 * 
 */
public class NNTrial {

	public static void main(String args[]) {
		int[] layers = { 2, 10, 1 };

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
		NeuralNetwork nn = NeuralNetwork.getNNInstance(random.nextLong(), layers,
				new QuadraticCost());
		for (Layer layer : nn.getNnLayers()) {
			layer.setActivation(new ReLU());
		}

		nn.setRegularization(REGULARIZATION.NONE);
		nn.setTrainingData(td);
		nn.stochasticGradientDescent(20, 750, 0.25);
		double[] ip = { 44.0, 34.3 };
		System.out.println("nnop:" + nn.feedForward(DenseVector.fromArray(ip)) + " real op:"
				+ fX(ip));
	}

	private static double fX(double[] x) {
		return 0.2 + 0.4 * x[0] + 0.3 * x[1];
	}

}
