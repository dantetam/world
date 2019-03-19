package io.github.dantetam.system;

import io.github.dantetam.lwjglEngine.toolbox.MousePicker;
import io.github.dantetam.render.GameLauncher;

public class RenderSystem extends BaseSystem {

	public MousePicker mousePicker;

	public RenderSystem(GameLauncher civGame) {
		super(civGame);
	}

	public void tick() {
		mousePicker.update();
	}

}
