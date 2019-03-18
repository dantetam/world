package io.github.dantetam.lwjglEngine.toolbox;

public class CustomMathUtil {

	public static double roundToPower2(double n) {
		return Math.pow(2, Math.round(Math.log10(n) / Math.log10(2)));
	}
	
}
