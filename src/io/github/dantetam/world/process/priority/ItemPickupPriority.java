package io.github.dantetam.world.process.priority;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;

public class ItemPickupPriority extends Priority {

	public InventoryItem item;
	
	public ItemPickupPriority(Vector3i coords, InventoryItem inv) {
		super(coords);
		item = inv;
	}

}
