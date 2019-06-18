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
	
}
