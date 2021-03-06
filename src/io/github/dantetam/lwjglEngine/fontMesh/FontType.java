package io.github.dantetam.lwjglEngine.fontMesh;

import java.io.File;

import io.github.dantetam.render.TextBox;

/**
 * Represents a font. It holds the font's texture atlas as well as having the
 * ability to create the quad vertices for any text using this font.
 * 
 * @author Karl
 *
 */
public class FontType {

	private int textureAtlas;
	private TextMeshCreator creator;

	/**
	 * Creates a new font and loads up the data about each character from the font
	 * file.
	 * 
	 * @param textureAtlas - the ID of the font atlas texture.
	 * @param fontFile     - the font file containing information about each
	 *                     character in the texture atlas.
	 */
	public FontType(int textureAtlas, File fontFile) {
		this.textureAtlas = textureAtlas;
		this.creator = new TextMeshCreator(fontFile);
	}

	/**
	 * @return The font texture atlas.
	 */
	public int getTextureAtlas() {
		return textureAtlas;
	}

	/**
	 * Takes in an unloaded text and calculate all of the vertices for the quads on
	 * which this text will be rendered. The vertex positions and texture coords and
	 * calculated based on the information from the font file.
	 * 
	 * @param text - the unloaded text.
	 * @return Information about the vertices of all the quads.
	 */
	public TextMeshData loadText(TextBox text) {
		return creator.createTextMesh(text);
	}

}
