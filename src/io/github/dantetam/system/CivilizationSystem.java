package io.github.dantetam.system;

import io.github.dantetam.render.GameLauncher;

public class CivilizationSystem extends BaseSystem {

	public boolean requestTurn = false;
	public int turnsPassed = 0;

	public CivilizationSystem(GameLauncher civGame) {
		super(civGame);
	}

	public void tick() {

	}

}
