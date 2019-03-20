package io.github.dantetam.world.dataparse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.dantetam.world.grid.InventoryItem;

public class ItemData {

	private static Map<Integer, InventoryItem> allItemsById = new HashMap<>();
	private static Map<String, Integer> itemNamesToIds = new HashMap<>();
	
	//Map group name to list of item ids in group e.g. Stone -> Basalt, Quartz, ...
	private static Map<String, List<Integer>> itemGroups = new HashMap<>();
	
	//Map item ids to the maximum amount allowed in one inventory space,
	//where negative numbers, 0, and 1 mean not stackable.
	private static Map<Integer, Integer> stackableMap = new HashMap<>();
	
	private static Map<Integer, Boolean> placeableBlock = new HashMap<>();
	
	public static InventoryItem createItem(int id, int quantity) {
		if (allItemsById.containsKey(id)) {
			InventoryItem item = allItemsById.get(id);
			return cloneItem(item, quantity);
		}
		return null;
	}
	
	public static InventoryItem cloneItem(InventoryItem item) {
		return cloneItem(item, item.quantity);
	}
	public static InventoryItem cloneItem(InventoryItem item, int quantity) {
		return new InventoryItem(item.id, quantity, item.name);
	}
	
	public static InventoryItem createItemByName(String name, int quantity) {
		if (itemNamesToIds.containsKey(name)) {
			int id = itemNamesToIds.get(name);
			return createItem(id, quantity);
		}
		return null;
	}
	
	public static void addItemToDatabase(int id, String name, boolean placeable, String[] groups, int stackable) {
		InventoryItem newItem = new InventoryItem(id, 0, name);
		allItemsById.put(id, newItem);
		itemNamesToIds.put(name, id);
		placeableBlock.put(id, placeable);
		for (String group: groups) {
			if (!group.isBlank()) {
				if (!itemGroups.containsKey(group)) {
					itemGroups.put(group, new ArrayList<>());
				}
				itemGroups.get(group).add(id);
			}
		}
		stackableMap.put(id, stackable);
	}
	
	
	
}
