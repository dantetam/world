package io.github.dantetam.lwjglEngine.gui;

public class Keyboard {

	public static boolean[] keys = new boolean[200];

	public static boolean isKeyDown(int key) {
		return keys[key];
	}

}
