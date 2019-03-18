package io.github.dantetam.system;

import io.github.dantetam.render.CivGame;

public class CivilizationSystem extends BaseSystem {

	public boolean requestTurn = false;
	public int turnsPassed = 0;

	public CivilizationSystem(CivGame civGame) {
		super(civGame);
	}

	public void tick() {

	}

}
