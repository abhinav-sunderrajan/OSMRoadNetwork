package main;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import org.la4j.Vector;
import org.la4j.vector.DenseVector;

import utils.TrainingData;

/**
 * 
 * @author abhinav.sunderrajan
 * 
 */
public class MNSITDataLoader {

	private int numLabels;
	private int numImages;
	private int numRows;
	private int numCols;
	private TrainingData td;

	private MNSITDataLoader() {
		td = new TrainingData();
	}

	/**
	 * 
	 * @param labelFilename
	 * @param imageFilename
	 * @return
	 */
	public static TrainingData getMNSITTrainingData(String labelFilename, String imageFilename) {
		MNSITDataLoader mnsit = new MNSITDataLoader();
		try {
			mnsit.loadMNSITTrainingData(labelFilename, imageFilename);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mnsit.td;
	}

	private void loadMNSITTrainingData(String labelFilename, String imageFilename)
			throws IOException {

		DataInputStream labels = new DataInputStream(new FileInputStream(labelFilename));
		DataInputStream images = new DataInputStream(new FileInputStream(imageFilename));
		int magicNumber = labels.readInt();
		if (magicNumber != 2049) {
			labels.close();
			images.close();
			throw new IllegalStateException("Label file has wrong magic number: " + magicNumber
					+ " (should be 2049)");

		}

		magicNumber = images.readInt();
		if (magicNumber != 2051) {
			labels.close();
			images.close();
			throw new IllegalStateException("Image file has wrong magic number: " + magicNumber
					+ " (should be 2051)");
		}

		numLabels = labels.readInt();
		numImages = images.readInt();
		numRows = images.readInt();
		numCols = images.readInt();
		if (numLabels != numImages) {
			StringBuilder str = new StringBuilder();
			str.append("Image file and label file do not contain the same number of entries.\n");
			str.append("  Label file contains: " + numLabels + "\n");
			str.append("  Image file contains: " + numImages + "\n");
			labels.close();
			images.close();
			throw new IllegalStateException(str.toString());
		}

		for (int i = 0; i < numLabels; i++) {
			// Since there are 10 digits to recognize we have 10 bits. We set
			// the bit corresponding to the label to 1.
			Vector label = DenseVector.zero(10);
			int num = labels.readByte();
			label.set(num, 1);
			td.getOutputs().add(label);
		}

		for (int i = 0; i < numImages; i++) {
			Vector imageVector = DenseVector.zero(numCols * numRows);
			for (int column = 0; column < numCols; column++) {
				for (int row = 0; row < numRows; row++) {
					int index = column * numRows + row;
					imageVector.set(index, (double) (images.readUnsignedByte() / 255.0));
				}
			}
			td.getInputs().add(imageVector);
		}

		images.close();
		labels.close();

	}
}
