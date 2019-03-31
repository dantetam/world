package io.github.dantetam.world.dataparse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.dantetam.lwjglEngine.render.VBOLoader;
import io.github.dantetam.world.dataparse.Process.ProcessStep;
import io.github.dantetam.world.items.InventoryItem;

public class ItemData {

	public static final int ITEM_EMPTY_ID = -1;
	private static int GENERATED_BASE_ID = 0; //Use the maximum id of all items plus 1
		//to generate new items with unique ids
	
	private static Map<Integer, InventoryItem> allItemsById = new HashMap<>();
	static Map<String, Integer> itemNamesToIds = new HashMap<>();
	
	//Map group name to list of item ids in group e.g. Stone -> Basalt, Quartz, ...
	private static Map<String, List<Integer>> itemGroups = new HashMap<>();
	private static Map<Integer, String> groupNameById = new HashMap<>();
	
	//Map item ids to the maximum amount allowed in one inventory space,
	//where negative numbers, 0, and 1 mean not stackable.
	private static Map<Integer, Integer> stackableMap = new HashMap<>();
	
	private static Map<Integer, Boolean> placeableBlock = new HashMap<>();
	private static Map<Integer, Integer> pickupTime = new HashMap<>();
	private static Map<Integer, Double> baseItemValue = new HashMap<>();
	
	private static Map<Integer, List<ProcessStep>> itemActionsById = new HashMap<>();
	
	private static Map<Integer, ItemTotalDrops> allItemDropsById = new HashMap<>();
	
	private static Map<Integer, Integer> refinedFormsById = new HashMap<>();
	
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
		return new InventoryItem(item.itemId, quantity, item.name);
	}
	
	public static int getIdFromName(String name) {
		if (!itemNamesToIds.containsKey(name)) {
			throw new IllegalArgumentException("Could not find item name: " + name);
		}
		return itemNamesToIds.get(name);
	}
	
	public static String getNameFromId(int id) {
		if (!allItemsById.containsKey(id)) {
			throw new IllegalArgumentException("Could not find item id: " + id);
		}
		return allItemsById.get(id).name;
	}
	
	public static InventoryItem createItemByName(String name, int quantity) {
		if (itemNamesToIds.containsKey(name)) {
			int id = itemNamesToIds.get(name);
			return createItem(id, quantity);
		}
		return null;
	}
	
	public static void addItemToDatabase(int id, String name, boolean placeable, 
			String[] groups, Integer stackable, int refinedForm, ItemTotalDrops itemTotalDrops,
			int time, double baseValue, List<ProcessStep> itemActions) {
		InventoryItem newItem = new InventoryItem(id, 0, name);
		allItemsById.put(id, newItem);
		itemNamesToIds.put(name, id);
		placeableBlock.put(id, placeable);
		if (groups != null) {
			for (String group: groups) {
				group = group.trim();
				if (!group.isBlank()) {
					if (!itemGroups.containsKey(group)) {
						itemGroups.put(group, new ArrayList<>());
					}
					itemGroups.get(group).add(id);
					groupNameById.put(id, group);
				}
			}
		}
		if (stackable != null && stackable > 1) {
			stackableMap.put(id, stackable);
		}
		if (refinedForm != ItemData.ITEM_EMPTY_ID) {
			refinedFormsById.put(id, refinedForm);
		}
		if (itemTotalDrops != null) {
			allItemDropsById.put(id, itemTotalDrops);
		}
		pickupTime.put(id, time);
		baseItemValue.put(id, baseValue);
		if (itemActions != null) {
			itemActionsById.put(id, itemActions);
		}
		GENERATED_BASE_ID = Math.max(GENERATED_BASE_ID, id + 1);
	}
	
	public static int generateItem(String name) {
		addItemToDatabase(GENERATED_BASE_ID, name, false, null, 15, ItemData.ITEM_EMPTY_ID, null, 100, 0.0, null);
		GENERATED_BASE_ID++;
		return GENERATED_BASE_ID - 1;
	}
	
	public static List<Integer> getGroupIds(String name) {
		if (!isGroup(name)) {
			throw new IllegalArgumentException("Could not find group name: " + name);
		}
		return itemGroups.get(name);
	}
	
	public static String getGroupNameById(int id) {
		return groupNameById.get(id);
	}
	 
	public static boolean isGroup(String name) {
		return itemGroups.containsKey(name);
	}

	public static ItemTotalDrops getOnBlockItemDrops(int id) {
		if (!allItemsById.containsKey(id)) {
			throw new IllegalArgumentException("Could not find item id: " + id);
		}
		return allItemDropsById.get(id);
	}
	
	public static boolean isPlaceable(int id) {
		if (!allItemsById.containsKey(id)) {
			throw new IllegalArgumentException("Could not find item id: " + id);
		}
		return placeableBlock.get(id);
	}
	
	public static int getPickupTime(Integer id) {
		if (!pickupTime.containsKey(id)) {
			throw new IllegalArgumentException("Could not find item id: " + id);
		}
		return pickupTime.get(id);
	}
	
	public static List<ProcessStep> getItemActions(Integer id) {
		return itemActionsById.get(id);
	}
	
	public static double getBaseItemValue(Integer id) {
		if (!baseItemValue.containsKey(id)) {
			throw new IllegalArgumentException("Could not find item id: " + id);
		}
		return baseItemValue.get(id);
	}
	
	public static int getRefinedFormId(Integer id) {
		return refinedFormsById.containsKey(id) ? refinedFormsById.get(id) : ItemData.ITEM_EMPTY_ID;
	}
	
	public static int getTextureFromItemId(int id) {
		String itemName = getNameFromId(id);
		return VBOLoader.loadTexture("res/tiles/" + itemName + ".png");
	}
	
}
