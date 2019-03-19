package io.github.dantetam.lwjglEngine.entities;

import io.github.dantetam.lwjglEngine.models.TexturedModel;
import io.github.dantetam.vector.CustomVector3f;

public class Entity {

	private TexturedModel model;
	public CustomVector3f position;
	public float rotX, rotY, rotZ;
	public float scale;

	public Entity(TexturedModel m, CustomVector3f p, float a, float b, float c, float s) {
		model = m;
		position = p;
		rotX = a;
		rotY = b;
		rotZ = c;
		scale = s;
	}

	public void move(float dx, float dy, float dz) {
		position.x += dx;
		position.y += dy;
		position.z += dz;
	}

	public void rotate(float dx, float dy, float dz) {
		rotX += dx;
		rotY += dy;
		rotZ += dz;
	}

	public TexturedModel getModel() {
		return model;
	}

}
