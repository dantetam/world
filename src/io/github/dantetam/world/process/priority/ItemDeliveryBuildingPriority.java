package io.github.dantetam.world.process.priority;

import io.github.dantetam.world.grid.LocalBuilding;
import io.github.dantetam.world.items.Inventory;

public class ItemDeliveryBuildingPriority extends Priority {

	public LocalBuilding building;
	public Inventory inventory;
	
	public ItemDeliveryBuildingPriority(LocalBuilding building, Inventory inv) {
		super(null);
		this.building = building;
		inventory = inv;
	}

}
