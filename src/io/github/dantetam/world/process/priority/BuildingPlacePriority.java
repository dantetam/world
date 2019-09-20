package io.github.dantetam.world.process.priority;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.LivingEntity;

public class BuildingPlacePriority extends Priority {

	public String buildingName;
	public LivingEntity owner;
	
	public BuildingPlacePriority(Vector3i coords, String name, LivingEntity owner) {
		super(coords);
		buildingName = name;
		this.owner = owner;
	}

}
