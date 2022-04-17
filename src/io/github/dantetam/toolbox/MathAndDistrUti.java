package io.github.dantetam.toolbox;

import java.text.DecimalFormat;

public class MathAndDistrUti {

	public static int trueMod(int a, int m) {
		int i = a % m;
		if (i < 0) i += m;
		return i;
	}

	public static double roundToPower2(double n) {
		return Math.pow(2, Math.ceil(Math.log10(n) / Math.log10(2)));
	}

	//Return a number from a to b, inclusive and equal chance across the range.
	public static int discreteUniform(int a, int b) {
		if (a > b) {
			int temp = a;
			a = b;
			b = temp;
		}
		int range = b - a + 1;
		return (int) (Math.random() * range) + a;
	}
	
	public static double relu(double a) {
		return a > 0 ? a : 0;
	}
	
	public static double clamp(double a, double min, double max) {
		return Math.max(min, Math.min(max, a));
	}
	
	public static String format(double d) {
		DecimalFormat df = new DecimalFormat("#.###");
		return df.format(d);
	}
	
}
