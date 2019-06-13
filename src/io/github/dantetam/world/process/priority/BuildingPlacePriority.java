package io.github.dantetam.world.process.priority;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.LivingEntity;

public class BuildingPlacePriority extends Priority {

	public InventoryItem buildingItem;
	public LivingEntity owner;
	
	public BuildingPlacePriority(Vector3i coords, InventoryItem id, LivingEntity owner) {
		super(coords);
		buildingItem = id;
		this.owner = owner;
	}

}
