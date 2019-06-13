package io.github.dantetam.lwjglEngine.entities;

import org.lwjgl.util.vector.Vector3f;

public class Camera {

	public Vector3f position = new Vector3f(500, 100, 500);
	public float pitch = -10, yaw = 0, roll = 0; // High-low, left-right, tilt

}
