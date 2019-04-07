package io.github.dantetam.world.process.priority;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;

public class BuildingPlacePriority extends Priority {

	public InventoryItem buildingItem;
	
	public BuildingPlacePriority(Vector3i coords, InventoryItem id) {
		super(coords);
		buildingItem = id;
	}

}
