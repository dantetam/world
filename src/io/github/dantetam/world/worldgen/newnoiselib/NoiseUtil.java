package io.github.dantetam.world.worldgen.newnoiselib;

public class NoiseUtil {

	public static boolean[][][] arrFloatToBool(float[][][] data, float cutoff) {
		return arrFloatToBool(data, cutoff, 0, 0);
	}
	
	public static boolean[][][] arrFloatToBool(float[][][] data, float cutoff, int centerHeight, float cutoffDropoff) {
		boolean[][][] boolData = new boolean[data.length][data[0].length][data[0][0].length];
		for (int z = 0; z < data[0][0].length; z++) {
			//float modCutoff = cutoff - heightRarity * (1f - (float) z / data[0][0].length);
			//modCutoff = (float) Math.max(modCutoff, cutoff * 0.5);
			float modCutoff = cutoff + Math.abs(z - centerHeight) * cutoffDropoff;
			for (int x = 0; x < data.length; x++) {
				for (int y = 0; y < data[0].length; y++) {
					boolData[x][y][z] = data[x][y][z] >= modCutoff;
				}
			}
		}
		return boolData;
	}
	
	public static float[][] normalizeFloat2d(float[][] data, float newMin, float newMax) {
		float mean = 0;
		for (int r = 0; r < data.length; r++) {
			for (int c = 0; c < data[0].length; c++) {
				mean += data[r][c];
			}
		}
		mean /= (data.length * data[0].length);
		
		float sd = 0;
		for (int r = 0; r < data.length; r++) {
			for (int c = 0; c < data[0].length; c++) {
				sd += Math.pow(data[r][c] - mean, 2);
			}
		}
		sd /= (data.length * data[0].length);
		
		float newMean = (newMin + newMax) / 2.0f;
		float newSd = (newMin + newMax) / 5.0f; //2.5 sd rule for 90ish% of data, assuming normality
		
		float[][] newData = new float[data.length][data[0].length];
		for (int r = 0; r < data.length; r++) {
			for (int c = 0; c < data[0].length; c++) {
				float numSds = (data[r][c] - mean) / sd;
				float newValue = newMean + newSd * numSds;
				newData[r][c] = newValue;
			}
		}
		return newData;
	}
	
}
