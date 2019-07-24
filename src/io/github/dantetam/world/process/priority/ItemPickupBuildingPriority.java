package io.github.dantetam.world.process.priority;

import io.github.dantetam.world.grid.LocalBuilding;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;

public class ItemPickupBuildingPriority extends Priority {

	public InventoryItem item;
	public LocalBuilding building;
	
	public ItemPickupBuildingPriority(LocalBuilding building, InventoryItem inv) {
		super(null);
		this.building = building;
		item = inv;
	}

}
