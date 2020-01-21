package io.github.dantetam.world.process.priority;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.life.BodyPart;
import io.github.dantetam.world.life.LivingEntity;

public class HealSelfPriority extends Priority {

	public LivingEntity being; //A being that is attempting to heal themselves
	public BodyPart part;
	
	public HealSelfPriority(Vector3i coords, LivingEntity being) {
		super(coords);
		this.being = being;
	}
	
}
