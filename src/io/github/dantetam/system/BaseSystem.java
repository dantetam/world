package io.github.dantetam.system;

import io.github.dantetam.render.GameLauncher;
import io.github.dantetam.world.grid.WorldGrid;

public abstract class BaseSystem {

	public GameLauncher gameLauncher;
	
	public BaseSystem(GameLauncher civGame) {
		gameLauncher = civGame;
	}

	public abstract void tick();

}
