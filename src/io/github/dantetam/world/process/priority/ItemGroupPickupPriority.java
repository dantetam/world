package io.github.dantetam.world.process.priority;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;

public class ItemGroupPickupPriority extends Priority {

	public String itemGroupName;
	public int amountNeeded;
	
	public ItemGroupPickupPriority(Vector3i coords, String name, int amt) {
		super(coords);
		itemGroupName = name;
		amountNeeded = amt;
	}

}
