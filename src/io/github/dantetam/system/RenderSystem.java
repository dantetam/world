package io.github.dantetam.system;

import io.github.dantetam.lwjglEngine.toolbox.MousePicker;
import io.github.dantetam.render.CivGame;

public class RenderSystem extends BaseSystem {

	public MousePicker mousePicker;

	public RenderSystem(CivGame civGame) {
		super(civGame);
	}

	public void tick() {
		mousePicker.update();
	}

}
