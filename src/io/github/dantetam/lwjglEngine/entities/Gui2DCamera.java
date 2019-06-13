package io.github.dantetam.lwjglEngine.entities;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.vector.Vector3f;

import io.github.dantetam.lwjglEngine.gui.Keyboard;

//There is no real camera in OpenGL
//Every object in the world must be moved in the opposite direction of the camera's movement

public class Gui2DCamera {

	public Vector3f tileLocationPosition; //In terms of coords: row, height, col
	public float numTilesX;
	public float numTilesZ;
	//private static final float LOWER_BOUND_Y = 40, UPPER_BOUND_Y = 400;
	public static float tileMoveLateralSpeed = 0.7f, tileHeightSpeed = 0.1f;

	public Gui2DCamera() {
		tileLocationPosition = new Vector3f(100,30,100);
		numTilesX = 32;
		numTilesZ = 20;
	}

	/**
	 * Move the camera with user key holds (from native JOGL bindings).
	 */
	public boolean move() {
		if (Keyboard.isKeyDown(GLFW.GLFW_KEY_I)) {
			tileLocationPosition.y += tileHeightSpeed;
		}
		if (Keyboard.isKeyDown(GLFW.GLFW_KEY_O)) {
			tileLocationPosition.y -= tileHeightSpeed; 
		}
		if (Keyboard.isKeyDown(GLFW.GLFW_KEY_A)) {
			tileLocationPosition.x -= tileMoveLateralSpeed;
		}
		if (Keyboard.isKeyDown(GLFW.GLFW_KEY_D)) {
			tileLocationPosition.x += tileMoveLateralSpeed;
		}
		if (Keyboard.isKeyDown(GLFW.GLFW_KEY_W)) {
			tileLocationPosition.z -= tileMoveLateralSpeed;
		}
		if (Keyboard.isKeyDown(GLFW.GLFW_KEY_S)) {
			tileLocationPosition.z += tileMoveLateralSpeed;
		}

		int[] inputKeys = new int[] { GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_W,
				GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_O };
		for (int inputKey : inputKeys) {
			if (Keyboard.isKeyDown(inputKey)) {
				//System.out.println(tileLocationPosition);
				return true;
			}
		}
		
		return false;
	}

}
