package io.github.dantetam.world.dataparse;

import java.util.HashMap;
import java.util.Map;

import io.github.dantetam.world.grid.InventoryItem;

public class ItemData {

	private static Map<Integer, InventoryItem> allItems = new HashMap<>();
	
	public InventoryItem createItem(int id, int quantity) {
		return new InventoryItem(id, quantity);
	}
	
}
