package io.github.dantetam.toolbox;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import io.github.dantetam.lwjglEngine.entities.Gui2DCamera;
import io.github.dantetam.lwjglEngine.gui.Mouse;
import io.github.dantetam.lwjglEngine.render.DisplayManager;
import io.github.dantetam.vector.Vector3i;

public class MousePicker {

	public Gui2DCamera camera;

	public MousePicker(Gui2DCamera camera) {
		this.camera = camera;
	}

	public Vector3i calculateWorldCoordsFromMouse() {
		int height = (int) Math.floor(camera.tileLocationPosition.y);
		int minX = (int) Math.floor(camera.tileLocationPosition.x - camera.numTilesX);
		int minZ = (int) Math.floor(camera.tileLocationPosition.z - camera.numTilesZ);
		//int maxX = (int) Math.ceil(camera.tileLocationPosition.x + camera.numTilesX);
		//int maxZ = (int) Math.ceil(camera.tileLocationPosition.z + camera.numTilesZ);
		
		float guiWidth = DisplayManager.width / (camera.numTilesX * 2 + 1);
		float guiHeight = DisplayManager.height / (camera.numTilesZ * 2 + 1);
		
		int tileScreenZeroIndexX = (int) (Mouse.getX() / guiWidth);
		int tileScreenZeroIndexZ = (int) (Mouse.getY() / guiHeight);
		tileScreenZeroIndexX += minX;
		tileScreenZeroIndexZ += minZ;
		
		return new Vector3i(tileScreenZeroIndexX, tileScreenZeroIndexZ, height);
	}

}
