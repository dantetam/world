package io.github.dantetam.world.dataparse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.lwjglEngine.render.VBOLoader;
import io.github.dantetam.toolbox.AlgUtil;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.grid.LocalBuilding;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.Process.ProcessStep;

public class ItemData {

	public static final int ITEM_EMPTY_ID = -1;
	private static int GENERATED_BASE_ID = 0; //Use the maximum id of all items plus 1
		//to generate new items with unique ids
	
	private static Map<Integer, InventoryItem> allItemsById = new HashMap<>();
	static Map<String, Integer> itemNamesToIds = new HashMap<>();
	
	//Map group name to list of item ids in group e.g. Stone -> Basalt, Quartz, ...
	private static Map<String, Set<Integer>> itemGroups = new HashMap<>();
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
	
	private static Map<Integer, List<Vector3i>> specialBuildingOffsets = new HashMap<>();
	private static Map<Integer, Vector2i> buildingSizes = new HashMap<>();
	private static Map<Integer, List<Integer>> specialBuildingBlockIds = new HashMap<>();
	
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
		if (id == ItemData.ITEM_EMPTY_ID) {
			return "EMPTY";
		}
		if (!allItemsById.containsKey(id)) {
			throw new IllegalArgumentException("Could not find item id: " + id);
		}
		return allItemsById.get(id).name;
	}
	
	public static InventoryItem item(String name, int quantity) {
		if (itemNamesToIds.containsKey(name)) {
			int id = itemNamesToIds.get(name);
			return createItem(id, quantity);
		}
		return null;
	}
	
	public static InventoryItem randomItem() {
		int id;
		do {
			Object[] ids = allItemsById.keySet().toArray();
			id = (Integer) ids[(int) (Math.random() * ids.length)];
		} while (placeableBlock.get(id) && groupNameById.get(id) != "Building");
		
		int maxStack = stackableMap.get(id); 
		int oneStackQuantity = (int) Math.ceil(Math.random() * maxStack * 0.75 + maxStack * 0.25);
		return createItem(id, oneStackQuantity);
	}
	
	public static void addItemToDatabase(int id, String name, boolean placeable, 
			String[] groups, Integer stackable, int refinedForm, ItemTotalDrops itemTotalDrops,
			int time, double baseValue, List<ProcessStep> itemActions, 
			List<Vector3i> specBuildOffsets) {
		InventoryItem newItem = new InventoryItem(id, 0, name);
		allItemsById.put(id, newItem);
		itemNamesToIds.put(name, id);
		placeableBlock.put(id, placeable);
		if (groups != null) {
			for (String group: groups) {
				group = group.trim();
				if (!group.isBlank()) {
					if (!itemGroups.containsKey(group)) {
						itemGroups.put(group, new HashSet<>());
					}
					itemGroups.get(group).add(id);
					groupNameById.put(id, group);
				}
			}
		}
		if (stackable != null) {
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
		
		if (itemGroups.get("Building").contains(id)) {
			if (specBuildOffsets != null && specBuildOffsets.size() > 0) {
				specialBuildingOffsets.put(id, specBuildOffsets);
				Vector3i[] pointBounds = AlgUtil.findCoordBounds(specBuildOffsets);
				Vector3i bounds = pointBounds[1].getSubtractedBy(pointBounds[0]);
				buildingSizes.put(id, new Vector2i(bounds.x, bounds.y));
			}
			else {
				buildingSizes.put(id, new Vector2i(1, 1));
			}
		}
		
		//Adjust this id so that new ids will always never conflict with the current items
		GENERATED_BASE_ID = Math.max(GENERATED_BASE_ID, id + 1); 
	}
	
	public static int generateItem(String name) {
		addItemToDatabase(GENERATED_BASE_ID, name, false, null, 15, ItemData.ITEM_EMPTY_ID, 
				null, 100, 0.0, null, null);
		GENERATED_BASE_ID++;
		return GENERATED_BASE_ID - 1;
	}
	
	public static Set<Integer> getGroupIds(String name) {
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
	
	public static int getMaxStackSize(Integer id) {
		if (!stackableMap.containsKey(id)) {
			throw new IllegalArgumentException("Could not find item id: " + id);
		}
		return stackableMap.get(id);
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
	
	public static LocalBuilding building(int id) {
		if (!allItemsById.containsKey(id)) {
			throw new IllegalArgumentException("Could not find item id: " + id);
		}
		String name = getNameFromId(id);
		LocalBuilding building;
		if (specialBuildingOffsets.containsKey(id)) {
			List<Vector3i> offsets = specialBuildingOffsets.get(id);
			List<Integer> blockIds = specialBuildingBlockIds.get(id);
			building = new LocalBuilding(name, null, offsets, blockIds);
		}
		else {
			List<Integer> singleIdList = Collections.singletonList(id);
			building = new LocalBuilding(name, null, null, singleIdList);
		}
		return building;
	}
	
	public static boolean isBuildingId(int id) {
		return buildingSizes.containsKey(id);
	}
	
	public static Vector2i buildingSize(int id) {
		if (!allItemsById.containsKey(id)) {
			throw new IllegalArgumentException("Could not find item id: " + id);
		}
		if (!isBuildingId(id)) {
			throw new IllegalArgumentException("Could not find building (as an item) id: " + id);
		}
		return buildingSizes.get(id);
	}
	
}
