package io.github.dantetam.system;

import io.github.dantetam.render.GameLauncher;

public abstract class BaseSystem {

	public GameLauncher main;

	public BaseSystem(GameLauncher civGame) {
		main = civGame;
	}

	public abstract void tick();

}
