package io.github.dantetam.world.process.priority;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civilization.LivingEntity;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.items.Inventory;

public class SoldierPriority extends Priority {

	public LivingEntity being;
	
	public SoldierPriority(Vector3i coords, LivingEntity being) {
		super(coords);
		this.being = being;
	}

	public static double weaponAmtRequired(LocalGrid grid, LivingEntity being) {
		
	}
	
	public static double armorAmtRequired(LocalGrid grid, LivingEntity being) {
		
	}
	
}
