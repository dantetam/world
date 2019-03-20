package io.github.dantetam.toolbox;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

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

	public static void writeBlendMapToFile(BufferedImage image, String fileName) {
		if (image == null)
			return;
		try {
			File file = new File(fileName);
			if (!file.exists())
				file.createNewFile();
			ImageIO.write(image, "png", file);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

}
