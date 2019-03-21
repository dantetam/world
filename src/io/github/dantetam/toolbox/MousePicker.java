package io.github.dantetam.toolbox;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import io.github.dantetam.lwjglEngine.entities.Gui2DCamera;

public class MousePicker {

	public Gui2DCamera camera;

	public MousePicker(Gui2DCamera camera) {
		this.camera = camera;
	}

	private Vector3f calculateTileClickedOn(float mouseX, float mouseY) {
		return null;
	}

	// This is the "normal" forward directed transformation from world space to
	// viewport space.
	// OpenGL automatically adds perspective division in its pipeline, so it is
	// included here
	// (and not in the inverse calculation).
	public Vector2f calculateScreenPos(Vector2f worldPosition) {
		return calculateScreenPos(worldPosition.x, worldPosition.y);
	}

	public Vector2f calculateScreenPos(float posX, float posY) {
		return null;
	}

}
