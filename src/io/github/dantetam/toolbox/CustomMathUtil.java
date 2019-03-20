package io.github.dantetam.toolbox;

public class CustomMathUtil {

	public static double roundToPower2(double n) {
		return Math.pow(2, Math.round(Math.log10(n) / Math.log10(2)));
	}
	
	public static void printTable(double[][] terrain) {
		for (int r = 0; r < terrain.length; r++) {
			for (int c = 0; c < terrain[0].length; c++) {
				String data = terrain[r][c] + "";
				//String data = String.format("%03d", (int) terrain[r][c]);
				/*
				if (terrain[r][c] <= 0) {
					data = "___";
				}
				*/
				System.out.print(data + " ");
			}
			System.out.println();
		}
	}

}
