package main;

import java.util.Random;

import network.NeuralNetwork;
import network.NeuralNetwork.REGULARIZATION;

import org.la4j.Matrix;
import org.la4j.Vector;
import org.la4j.vector.DenseVector;

import utils.TrainingData;
import costfunction.CrossEntropyCost;

/**
 * 
 * @author abhinav.sunderrajan
 * 
 */
public class XOR {

	public static void main(String args[]) {
		int[] layers = { 2, 5, 1 };

		Random random = new Random();
		TrainingData td = new TrainingData();

		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				double[] ip = { i, j };
				Vector input = DenseVector.fromArray(ip);
				double op[] = { i ^ j };
				Vector output = DenseVector.fromArray(op);
				td.addTrainingData(input, output);
			}
		}

		NeuralNetwork nn = NeuralNetwork.getNNInstance(random.nextLong(), layers);

		nn.setRegularization(REGULARIZATION.NONE);
		nn.setTrainingData(td);

		Matrix weights[] = new Matrix[nn.getNumOfLayers()];
		for (int i = 0; i < weights.length; i++)
			weights[i] = nn.getNnLayers()[i].getWeight();

		nn.setCostFunction(new CrossEntropyCost(REGULARIZATION.NONE, weights, nn.getTd()
				.getInputs().size(), 5.0));
		nn.stochasticGradientDescent(4, 800, 2.0);

		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				double[] ip = { i, j };
				Vector input = DenseVector.fromArray(ip);
				System.out.println(nn.feedForward(input));
			}
		}

	}

}
