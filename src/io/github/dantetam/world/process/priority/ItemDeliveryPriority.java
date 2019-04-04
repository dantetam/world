package io.github.dantetam.world.process.priority;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.items.Inventory;

public class ItemDeliveryPriority extends Priority {

	public Inventory inventory;
	
	public ItemDeliveryPriority(Vector3i coords, Inventory inv) {
		super(coords);
		inventory = inv;
	}

}
