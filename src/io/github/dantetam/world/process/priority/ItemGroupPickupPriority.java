package io.github.dantetam.world.process.priority;

import java.util.List;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.items.GroupItem;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;

public class ItemGroupPickupPriority extends Priority {

	public List<GroupItem> needs;
	
	public ItemGroupPickupPriority(Vector3i coords, List<GroupItem> needs) {
		super(coords);
		this.needs = needs;
	}

}
