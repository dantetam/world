package io.github.dantetam.world.worldgen.newnoiselib;

public class NoiseUtil {

	public static boolean[][][] convertFloatToBool(float[][][] data, float cutoff) {
		boolean[][][] boolData = new boolean[data.length][data[0].length][data[0][0].length];
		for (int x = 0; x < data.length; x++) {
			for (int y = 0; y < data[0].length; y++) {
				for (int z = 0; z < data[0][0].length; z++) {
					boolData[x][y][z] = data[x][y][z] >= cutoff;
				}
			}
		}
		return boolData;
	}
	
}
