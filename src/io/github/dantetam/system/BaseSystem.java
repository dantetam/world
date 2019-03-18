package io.github.dantetam.system;

import io.github.dantetam.render.CivGame;

public abstract class BaseSystem {

	public CivGame main;
	
	public BaseSystem(CivGame civGame)
	{
		main = civGame;
	}
	
	public abstract void tick();
	
}
