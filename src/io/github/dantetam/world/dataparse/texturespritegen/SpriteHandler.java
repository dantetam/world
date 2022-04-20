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
	protected String fileName;
	protected int spriteWidth, spriteHeight;
	protected abstract Map<String, SpriteSheetInstruction> getItemsToSpriteCoordsMap();
	
	private static BufferedImage spriteSheetImage;
	
	public static void init() {
		new AyeneSpriteHandler().initThisSpriteHandler();
		new ShikashiSpriteHandler().initThisSpriteHandler();
	}
	
	public void getBufferedImage() {
		if (fileName == null) throw new InstantiationError("Sprite sheet needs to have its filename set");
		try {
			File file = new File(fileName);
			spriteSheetImage = ImageIO.read(file);
		} catch (IOException e) {
			CustomLog.errPrintln("Could not load sprite sheet in file location: " + fileName);
		}
	}
	
	protected void loadAllSpriteTextures() {
		Map<String, SpriteSheetInstruction> spriteMap = getItemsToSpriteCoordsMap();
		if (spriteMap == null) {
			CustomLog.errPrintln("Warning, sprite handler has no itemsToSpriteCoordsMap");
			return;
		}
		for (Entry<String, SpriteSheetInstruction> entry : spriteMap.entrySet()) {
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
			System.err.println("Attempt to save file: " + itemName);
		    try {
				ImageIO.write(newImg, "png", outputFile);
				System.err.println("Saved item to res/spritesetsTesting" + itemName + ".png");
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
				if (RGBUtil.colorDifference(pixel, colorTransform.baseColor) < colorTransform.colorDiffTolerance) {
					newPixels[i] = RGBUtil.blendColors(
							colorTransform.newColor, pixel, 0.75);
				} else {
					newPixels[i] = pixels[i];
				}
			}
		}
		return newPixels;
	}
	
	protected void initThisSpriteHandler() {
		
	}
	
	protected static class SpriteSheetInstruction {
		//Given a width of 32x32 icons, [2,3] represents 2 rows down and 3 columns across 
		//so the icon would expand from (64, 32) to (96, 64)
		//The top left is [1,1]
		public Vector2i coords;
		public List<ColorTransform> colorsTransform;
		public SpriteSheetInstruction(int a, int b) {
			this(new Vector2i(a,b));
		}
		public SpriteSheetInstruction(Vector2i coords) {
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
