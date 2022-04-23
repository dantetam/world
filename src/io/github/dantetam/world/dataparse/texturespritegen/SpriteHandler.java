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
import io.github.dantetam.toolbox.RGBUtil;
import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.vector.Vector2i;

/**
 * 
 * General class for creating sprites from sprite sheets, using sprite coordinates
 * 
 * @author Dante
 *
 */

public abstract class SpriteHandler {

	//Set these data fields in subclasses i.e. sprite sheets based on a single file
	protected abstract int getSpriteWidth();
	protected abstract int getSpriteHeight();
	protected abstract Map<String, SpriteSheetInstruction> getItemsToSpriteCoordsMap();
	
	private static Map<String, BufferedImage> spriteSheetImages;
	
	public static void init() {
		new TerrainSpriteHandler().loadAllSpriteTextures();
		new ToolSpriteHandler().loadAllSpriteTextures();
	}
	
	public BufferedImage getBufferedImage(String fileName) {
		if (spriteSheetImages == null) spriteSheetImages = new HashMap<>();
		if (spriteSheetImages.containsKey(fileName)) return spriteSheetImages.get(fileName);
		if (fileName == null) throw new InstantiationError("Sprite sheet needs to have its filename set");
		try {
			File file = new File(fileName);
			BufferedImage spriteSheetImage = ImageIO.read(file);
			if (spriteSheetImage == null) {
				throw new IllegalArgumentException("Had trouble reading in sprite sheet: " + fileName);
			}
			spriteSheetImages.put(fileName, spriteSheetImage);
			return spriteSheetImage;
		} catch (IOException e) {
			CustomLog.errPrintln("Could not load sprite sheet in file location: " + fileName);
		}
		return null;
	}
	
	protected void loadAllSpriteTextures() {
		Map<String, SpriteSheetInstruction> spriteMap = getItemsToSpriteCoordsMap();
		if (spriteMap == null) {
			CustomLog.errPrintln("Warning, sprite handler has no itemsToSpriteCoordsMap");
			return;
		}
		int spriteWidth = getSpriteWidth(), spriteHeight = getSpriteHeight();
		for (Entry<String, SpriteSheetInstruction> entry : spriteMap.entrySet()) {
			String itemName = entry.getKey();
			SpriteSheetInstruction instruction = entry.getValue();
			Vector2i coords = instruction.coords;
			
			int cols = coords.y, rows = coords.x;
			int startY = (rows - 1) * spriteHeight, startX = (cols - 1) * spriteWidth;
			
			int[] pixels = new int[spriteWidth * spriteHeight];
			
			BufferedImage spriteSheetImage = getBufferedImage(instruction.spriteFileName);
			try {
				spriteSheetImage.getRGB(startX, startY, spriteWidth, spriteHeight, pixels, 0, spriteWidth);			
			} catch (ArrayIndexOutOfBoundsException e) {
				CustomLog.errPrintln("Attempted sprite data: " + instruction.spriteFileName + " at coords: " + coords);
				CustomLog.outPrintlnArr(new Integer[] {startX, startY, spriteWidth, spriteHeight, 0, spriteWidth});
				CustomLog.errPrintln("Ensure the correct sprite map and coordinates are being used.");
				continue;
			}
			
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
				//System.err.println("Saved item to res/spritesetsTesting/" + itemName + ".png");
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
				if (RGBUtil.colorDifference(pixel, colorTransform.baseColor) 
						< colorTransform.colorDiffTolerance && newPixels[i] == 0) {
					newPixels[i] = RGBUtil.blendColors(
							colorTransform.newColor, pixel, 0.5);
					//TODO: Blend colors with varying (chosen) weighting, and increase contrast of orig. color
					//if new color is heavily weighted
				}
			}
			if (newPixels[i] == 0) {
				newPixels[i] = pixels[i];
			}
		}
		return newPixels;
	}
	
	//Shorthand method for RGB conversion
	protected int rgb(int r, int g, int b) {
		return RGBUtil.getIntColor(r, g, b);
	}
	protected int rgb(int s) {
		return RGBUtil.getIntColor(s, s, s);
	}
	
	//Shorthand for editing multiple textures at once
	protected static void listAddTexture(Map<String, SpriteSheetInstruction> itemsToSpriteCoordsMap, 
			List<String> list, SpriteSheetInstruction inst) {
		for (String itemName : list) {
			if (!itemsToSpriteCoordsMap.containsKey(itemName)) {
				itemsToSpriteCoordsMap.put(itemName, inst);
			}
		}
	}
	
	protected static class SpriteSheetInstruction {
		public String spriteFileName;
		
		//Given a width of 32x32 icons, [2,3] represents 2 rows down and 3 columns across 
		//so the icon would expand from (64, 32) to (96, 64)
		//The top left is [1,1]
		public Vector2i coords;
		public List<ColorTransform> colorsTransform;
		public SpriteSheetInstruction(String spriteFileName, int a, int b) {
			this(spriteFileName, new Vector2i(a,b));
		}
		public SpriteSheetInstruction(String spriteFileName, Vector2i coords) {
			this.spriteFileName = spriteFileName;
			this.coords = coords;
			this.colorsTransform = new ArrayList<>();
		}
		public SpriteSheetInstruction shade(int baseColor, int newColor, double colorDiffTolerance) {
			colorsTransform.add(new ColorTransform(baseColor, newColor, colorDiffTolerance));
			return this;
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
