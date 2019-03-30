package io.github.dantetam.world.civilization;

import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.items.Inventory;

public class Human extends Person {

	public int hydration, maxHydration, nutrition, maxNutrition, rest, maxRest;
	
	public Human(String name) {
		super(name);
	}

}
