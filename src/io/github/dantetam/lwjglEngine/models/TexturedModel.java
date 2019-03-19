package io.github.dantetam.lwjglEngine.models;

import io.github.dantetam.lwjglEngine.textures.LoadedIdTexture;

public class TexturedModel {

	private RawModel rawModel;
	private LoadedIdTexture texture;

	public TexturedModel(RawModel m, LoadedIdTexture t) {
		rawModel = m;
		texture = t;
	}

	public RawModel getRawModel() {
		return rawModel;
	}

	public LoadedIdTexture getTexture() {
		return texture;
	}

}
