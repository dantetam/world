package io.github.dantetam.world.dataparse.texturespritegen;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import io.github.dantetam.lwjglEngine.render.VBOLoader;
import io.github.dantetam.toolbox.DiffPair;
import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.vector.Vector2i;

/**
 * 
 * General class for creating sprites from sprite sheets, using sprite coordinates
 * 
 * @author Dante
 *
 */

public class SpriteHandler {

	//Set these data fields in subclasses i.e. sprite sheets based on a single file
	protected static String fileName;
	protected static int spriteWidth, spriteHeight;
	protected static Map<String, SpriteSheetInstruction> itemsToSpriteCoordsMap;
	
	private static BufferedImage spriteSheetImage;
	
	public static void init() {
		ShikashiSpriteHandler.loadAllSpriteTextures();
	}
	
	public static void getBufferedImage() {
		if (fileName == null) throw new InstantiationError("Sprite sheet needs to have its filename set");
		try {
			File file = new File(fileName);
			spriteSheetImage = ImageIO.read(file);
		} catch (IOException e) {
			CustomLog.errPrintln("Could not load sprite sheet in file location: " + fileName);
		}
	}
	
	protected static void loadAllSpriteTextures() {
		for (Entry<String, SpriteSheetInstruction> entry : itemsToSpriteCoordsMap.entrySet()) {
			String itemName = entry.getKey();
			SpriteSheetInstruction instruction = entry.getValue();
			Vector2i coords = instruction.coords;
			
			int cols = coords.y, rows = coords.x;
			int startY = (rows - 1) * spriteHeight, startX = (cols - 1) * spriteWidth;
			
			int[] pixels = new int[spriteWidth * spriteHeight];
			spriteSheetImage.getRGB(startX, startY, spriteWidth, spriteHeight, pixels, 0, spriteWidth);
			pixels = transformPixels(pixels, instruction.colorsTransform);
			
			BufferedImage newImg = new BufferedImage(spriteWidth, spriteHeight, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < spriteHeight; y++) {
				for (int x = 0; x < spriteWidth; x++) {
					newImg.setRGB(x, y, pixels[y * spriteWidth + x]);
				}
			}
			File outputFile = new File("res/spritesetsTesting/" + itemName + ".png");
		    try {
				ImageIO.write(newImg, "png", outputFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			VBOLoader.loadTextureImageCoords(pixels, itemName, spriteWidth, spriteHeight,
					false, true);
		}
	}
	
	private static int[] transformPixels(int[] pixels, Collection<ColorTransform> colorTransforms) {
		int[] newPixels = new int[pixels.length];
		for (int i = 0; i < pixels.length; i++) {
			int pixel = pixels[i];
			for (ColorTransform colorTransform : colorTransforms) {
				if (colorDifference(pixel, colorTransform.baseColor) < colorTransform.colorDiffTolerance) {
					newPixels[i] = colorTransform.newColor;
				} else {
					newPixels[i] = pixels[i];
				}
			}
		}
		return newPixels;
	}
	
	public static double colorDifference(int rgb1, int rgb2) {
		double red1 = ((rgb1 >> 16) & 0xFF);
		double red2 = ((rgb2 >> 16) & 0xFF);
		
		double dr = red1 - red2;
		double dg = ((rgb1 >> 8) & 0xFF) - ((rgb2 >> 8) & 0xFF);
		double db = (rgb1 & 0xFF) - (rgb2 & 0xFF);
		//double da = ((rgb1 >> 24) & 0xFF) - ((rgb2 >> 24) & 0xFF);
		
		double ravg = (red1 + red2) / 2.0;
		if (ravg < 128) {
			return Math.sqrt(2.0*dr + 4.0*dg + 3.0*db);
		} else {
			return Math.sqrt(3.0*dr + 4.0*dg + 2.0*db);
		}
	}
	
	protected static class SpriteSheetInstruction {
		//Given a width of 32x32 icons, [2,3] represents 2 rows down and 3 columns across 
		//so the icon would expand from (64, 32) to (96, 64)
		public Vector2i coords;
		public List<ColorTransform> colorsTransform;
		public SpriteSheetInstruction(Vector2i coords) {
			this.coords = coords;
			this.colorsTransform = new ArrayList<>();
		}
	}
	
	protected static class ColorTransform {
		public int baseColor;
		public int newColor;
		public double colorDiffTolerance;
		public ColorTransform(int baseColor, int newColor, double colorDiffTolerance) {
			this.baseColor = baseColor;
			this.newColor = newColor;
			this.colorDiffTolerance = colorDiffTolerance;
		}
	}
	
}
