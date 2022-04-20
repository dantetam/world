package io.github.dantetam.lwjglEngine.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import io.github.dantetam.lwjglEngine.models.RawModel;
import io.github.dantetam.toolbox.log.CustomLog;

public class VBOLoader {

	// Store VAOs and VBOs indices as reference for future clean up
	private static ArrayList<Integer> vaos = new ArrayList<Integer>();
	private static ArrayList<Integer> vbos = new ArrayList<Integer>();

	private static Map<String, Integer> textureNames = new HashMap<>();

	// Create a new model from float data, which accessed by the renderer
	public static RawModel loadToVAO(float[] pos, float[] textureCoords, float[] normals, int[] indices) {
		int vaoID = createVAO();
		bindIndicesBuffer(indices);
		storeData(0, 3, pos); // Store the position (3-tuples) in pos 0 of the VAO
		storeData(1, 2, textureCoords);
		storeData(2, 3, normals);
		unbindVAO();
		// There are repeats in the old pos[] so indices now contains the correct number
		// of indices
		return new RawModel(vaoID, indices.length);
	}

	// For 2D text and GUIs
	public static int loadToVAO(float[] positions, float[] textureCoords) {
		int vaoID = createVAO();
		storeData(0, 2, positions);
		storeData(1, 2, textureCoords);
		unbindVAO();
		return vaoID;
	}

	// For basic GUI tests
	public static RawModel loadToVAO(float[] positions) {
		int vaoID = createVAO();
		storeData(0, 2, positions);
		unbindVAO();
		return new RawModel(vaoID, positions.length / 2);
	}

	public static int loadTexture(String itemName, String fileName) {
		return loadTexture(itemName, fileName, false, true);
	}

	public static int loadTexture(String itemName, String fileName, boolean overrideStoredFileStr, boolean advancedRender) {
		if (textureNames.containsKey(itemName) && !overrideStoredFileStr) // Find texture if already loaded
			return textureNames.get(itemName);
		
		BufferedImage image = null;
		try {
			File file = new File(fileName);
			image = ImageIO.read(file);
		} catch (IOException e) {
			try {
				File file = new File("res/" + fileName + ".png");
				image = ImageIO.read(file);
			} catch (IOException e2) {
				CustomLog.outPrintln("Could not load " + fileName + ", loading from default texture instead");
				try {
					File file = new File("res/tiles/Error.png");
					image = ImageIO.read(file);
				} catch (IOException e3) {
					e3.printStackTrace();
				}
			}
		}
		return loadTextureImageCoords(image, itemName, 0, 0, image.getWidth(), image.getHeight(),
				overrideStoredFileStr, advancedRender);
	}
	
	public static int loadTextureImageCoords(BufferedImage image, String itemName, 
			int startX, int startY, int width, int height,
			boolean overrideStoredFileStr, boolean advancedRender) {
		if (textureNames.containsKey(itemName) && !overrideStoredFileStr) // Find texture if already loaded
			return textureNames.get(itemName);

		int[] pixels = new int[width * height];
		image.getRGB(startX, startY, width, height, pixels, 0, width);

		return loadTextureImageCoords(pixels, itemName, width, height, overrideStoredFileStr, advancedRender);
	}
	
	/**
	 * Load full image into texture and memotize it
	 */
	public static int loadTextureImageCoords(int[] pixels, String itemName, 
			int width, int height, boolean overrideStoredFileStr, boolean advancedRender) {
		if (textureNames.containsKey(itemName) && !overrideStoredFileStr) // Find texture if already loaded
			return textureNames.get(itemName);

		final int BYTES_PER_PIXEL = 4;

		ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * BYTES_PER_PIXEL); 
		// 4 for RGBA, 3 for RGB
		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int pixel = pixels[y * width + x];
				buffer.put((byte) ((pixel >> 16) & 0xFF));
				buffer.put((byte) ((pixel >> 8) & 0xFF));
				buffer.put((byte) (pixel & 0xFF));
				buffer.put((byte) ((pixel >> 24) & 0xFF));
			}
		}
		buffer.flip();

		int textureID = GL11.glGenTextures(); // Generate texture ID
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID); // Bind texture ID

		// Setup wrap mode for texture parameters
		// GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S,
		// GL11.GL_CLAMP);
		// GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T,
		// GL11.GL_CLAMP);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

		// Setup texture scaling filtering
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

		// Send texel data to OpenGL
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA,
				GL11.GL_UNSIGNED_BYTE, buffer);

		// Mipmap creation in native JOGL
		if (advancedRender) {
			// Setup texture scaling filtering
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

			GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
			GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0f);

			// Anisotropic filtering (also native but non-core OpenGL)
			float anisotropicFilterAmount = Math.min(4f, EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
			GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT,
					anisotropicFilterAmount);
		}

		// Store later for deleting
		textureNames.put(itemName, textureID);
		
		// Return the texture ID so we can bind it later again
		return textureID;
	}

	// Delete VAOs and VBOs by finding their vertices
	public static void cleanData() {
		for (int i : vaos)
			GL30.glDeleteVertexArrays(i);
		for (int i : vbos)
			GL15.glDeleteBuffers(i);
		for (int i : textureNames.values())
			GL11.glDeleteTextures(i);
	}

	// Request a new VAO id, store that ID, and bind it
	private static int createVAO() {
		int vaoID = GL30.glGenVertexArrays();
		vaos.add(vaoID);
		GL30.glBindVertexArray(vaoID);
		return vaoID;
	}

	private static void storeData(int attribNum, int coordinateSize, float[] data) {
		int vboID = GL15.glGenBuffers(); // Request a VBO id
		vbos.add(vboID);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID); // Bind it for use
		FloatBuffer buffer = toFloatBuffer(data); // Convert the data to a float buffer

		// Store the buffer in the VBO, for use in a static draw (vs. a dynamic draw)
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

		// Indicate that triangles are being drawn
		GL20.glVertexAttribPointer(attribNum, coordinateSize, GL11.GL_FLOAT, false, 0, 0);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0); // Unbind the current VBO being used
	}

	// Unbind the current VAO being used
	private static void unbindVAO() {
		GL30.glBindVertexArray(0);
	}

	private static void bindIndicesBuffer(int[] indices) {
		int vboID = GL15.glGenBuffers(); // Request a VBO id
		vbos.add(vboID);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboID); // Note "ELEMENT_ARRAY" not "ARRAY"
		IntBuffer buffer = toIntBuffer(indices);
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW); // Store the data in the bound VBO
	}

	// Convert arrays of numbers to the respective buffers
	private static IntBuffer toIntBuffer(int[] data) {
		IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
		buffer.put(data);
		buffer.flip();
		return buffer;
	}

	private static FloatBuffer toFloatBuffer(float[] data) {
		FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
		buffer.put(data);
		buffer.flip();
		return buffer;
	}

}
