package io.github.dantetam.toolbox;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.vector.Vector3i;

public class RGBUtil {

	public static int getIntColor(int r, int g, int b) {
		r = r < 0 ? 0 : r;
		g = g < 0 ? 0 : g;
		b = b < 0 ? 0 : b;
		r = r > 255 ? 255 : r;
		g = g > 255 ? 255 : g;
		b = b > 255 ? 255 : b;
		int col = (r << 16) | (g << 8) | b;
		return col;
	}
	
	public static int getIntColor(int r, int g, int b, int a) {
		r = r < 0 ? 0 : r;
		g = g < 0 ? 0 : g;
		b = b < 0 ? 0 : b;
		a = a < 0 ? 0 : a;
		r = r > 255 ? 255 : r;
		g = g > 255 ? 255 : g;
		b = b > 255 ? 255 : b;
		a = a > 255 ? 255 : a;
		int col = (a << 24) | (r << 16) | (g << 8) | b;
		return col;
	}

	public static void writeBlendMapToFile(BufferedImage image, String fileName) {
		if (image == null)
			return;
		try {
			File file = new File(fileName);
			if (!file.exists())
				file.createNewFile();
			ImageIO.write(image, "png", file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static double colorDifference(int rgb1, int rgb2) {
		double red1 = ((rgb1 >> 16) & 0xFF);
		double red2 = ((rgb2 >> 16) & 0xFF);
		
		double dr = red1 - red2;
		double dg = ((rgb1 >> 8) & 0xFF) - ((rgb2 >> 8) & 0xFF);
		double db = (rgb1 & 0xFF) - (rgb2 & 0xFF);
		//double da = ((rgb1 >> 24) & 0xFF) - ((rgb2 >> 24) & 0xFF);
		
		dr = Math.pow(dr, 2); dg = Math.pow(dg, 2); db = Math.pow(db, 2); 
		
		double ravg = (red1 + red2) / 2.0;
		if (ravg < 128) {
			return Math.sqrt(2.0*dr + 4.0*dg + 3.0*db);
		} else {
			return Math.sqrt(3.0*dr + 4.0*dg + 2.0*db);
		}
	}
	
	public static int blendColors(int rgb1, int rgb2, double weightToFirst) {
		double red1 = (rgb1 >> 16) & 0xFF, red2 = (rgb2 >> 16) & 0xFF;
		double green1 = (rgb1 >> 8) & 0xFF, green2 = (rgb2 >> 8) & 0xFF;
		double blue1 = rgb1 & 0xFF, blue2 = rgb2 & 0xFF;
		double alpha1 = (rgb1 >> 24) & 0xFF, alpha2 = (rgb2 >> 24) & 0xFF;
		double newR = red1 * weightToFirst + red2 * (1 - weightToFirst);
		double newG = green1 * weightToFirst + green2 * (1 - weightToFirst);
		double newB = blue1 * weightToFirst + blue2 * (1 - weightToFirst);
		double newA = alpha1 * weightToFirst + alpha2 * (1 - weightToFirst);
		return getIntColor((int) newR, (int) newG, (int) newB, (int) newA);
	}
	
	public static Vector3i getRgb(int color) {
		return new Vector3i(
				(color >> 16) & 0xFF,
				(color >> 8) & 0xFF,
				color & 0xFF
				);
	}
	
	public static void main(String[] args) {
		CustomLog.errPrintln(getRgb(blendColors(
				getIntColor(255, 125, 0),
				getIntColor(0, 0, 0),
				0.5
				)));
		
		CustomLog.errPrintln(colorDifference(
				getIntColor(255,0,0), getIntColor(0,255,0)
				));
	}

}
