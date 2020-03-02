package io.github.dantetam.world.process.priority;

import java.util.Map;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;

/**
 * A placeholder priority for when group item requests are made
 * This is a reference to actual items within an inventory
 * that can be used for any purpose.
 * @author Dante
 *
 */

public class FoundItemForUsePriority extends Priority {

	public Map<InventoryItem, Integer> foundItemsMap;
	
	public FoundItemForUsePriority(Vector3i coords, Map<InventoryItem, Integer> items) {
		super(coords);
		this.foundItemsMap = items;
	}

}
