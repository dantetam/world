package io.github.dantetam.world.dataparse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.localdata.ConstantData;
import io.github.dantetam.lwjglEngine.render.VBOLoader;
import io.github.dantetam.toolbox.Pair;
import io.github.dantetam.toolbox.VecGridUtil;
import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.grid.LocalBuilding;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.ProcessStep;

public class ItemData {

	public static final int ITEM_EMPTY_ID = -1;
	private static int GENERATED_BASE_ID = 0; //Use the maximum id of all items plus 1
		//to generate new items with unique ids
	
	private static Map<Integer, InventoryItem> allItemsById = new HashMap<>();
	static Map<String, Integer> itemNamesToIds = new HashMap<>();
	
	//Map group name to list of item ids in group e.g. Stone -> Basalt_id, Quartz_id, ...
	private static Map<String, Set<Integer>> itemGroups = new HashMap<>();
	private static Map<Integer, Set<String>> groupNameById = new HashMap<>();
	
	//Map item ids to the maximum amount allowed in one inventory space,
	//where negative numbers, 0, and 1 mean not stackable.
	private static Map<Integer, Integer> stackableMap = new HashMap<>();
	
	private static Map<Integer, Boolean> placeableBlock = new HashMap<>();
	private static Map<Integer, Integer> pickupTime = new HashMap<>();
	private static Map<Integer, Double> baseItemValue = new HashMap<>();
	private static Map<Integer, Double> beautyItemValue = new HashMap<>();
	
	private static Map<Integer, List<ProcessStep>> itemActionsById = new HashMap<>();
	private static Map<Integer, List<ProcessStep>> itemPropertiesById = new HashMap<>();
	
	private static Map<String, Set<Integer>> itemActionsNamed = new HashMap<>();
	private static Map<String, Set<Integer>> itemPropertiesNamed = new HashMap<>();
	
	private static Map<Integer, ItemTotalDrops> allItemDropsById = new HashMap<>();
	//private static Map<Integer, Map<Integer, Double>> itemDropExpectationById = new HashMap<>();
	
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
	
	public static InventoryItem createItemFromName(String name, int quantity) {
		if (!itemNamesToIds.containsKey(name)) {
			throw new IllegalArgumentException("Cannot create item instance from name: " + name);
		}
		int id = itemNamesToIds.get(name);
		InventoryItem item = allItemsById.get(id);
		return cloneItem(item, quantity);
	}
	
	public static InventoryItem cloneItem(InventoryItem item) {
		return cloneItem(item, item.quantity);
	}
	public static InventoryItem cloneItem(InventoryItem item, int quantity) {
		return new InventoryItem(item.itemId, quantity, item.quality, item.name);
	}
	
	public static Collection<InventoryItem> getAllItems() {
		return allItemsById.values();
	}
	
	public static int getIdFromName(String name) {
		if (name.contains("<") && name.contains(">")) {
			throw new IllegalArgumentException("Gave group name: " + name + " for item id, perhaps use getIdsFromName(Stirng name)");
		}
		if (!itemNamesToIds.containsKey(name)) {
			throw new IllegalArgumentException("Could not find item name: " + name);
		}
		return itemNamesToIds.get(name);
	}
	
	
	public static Set<Integer> getIdsFromNameOrGroup(String name) {
		Set<Integer> list = new HashSet<>();
		if (name.contains("<") && name.contains(">")) {
			return itemGroups.get(WorldCsvParser.getGroupNameFromStr(name));
		}
		else if (isGroup(name)) {
			return itemGroups.get(name);
		}
		else {
			list.add(getIdFromName(name));
			return list;
		}
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
	
	public static boolean isValidId(int id) {
		return allItemsById.containsKey(id);
	}
	
	public static InventoryItem item(String name, int quantity) {
		if (itemNamesToIds.containsKey(name)) {
			int id = itemNamesToIds.get(name);
			return createItem(id, quantity);
		}
		throw new IllegalArgumentException("Could not find item name: " + name);
	}
	
	public static InventoryItem randomItem() {
		int id;
		do {
			Object[] ids = allItemsById.keySet().toArray();
			id = (Integer) ids[(int) (Math.random() * ids.length)];
		} while (placeableBlock.get(id) && groupNameById.get(id) != null && 
				groupNameById.get(id).contains("Building"));
		
		int maxStack = stackableMap.get(id); 
		int oneStackQuantity = (int) Math.ceil(Math.random() * maxStack * 0.75 + maxStack * 0.25);
		return createItem(id, oneStackQuantity);
	}
	
	public static InventoryItem randomBaseItem() {
		InventoryItem item;
		while (true) {
			item = randomItem();
			double value = getBaseItemValue(item.itemId);
			if (value <= 8) {
				return item;
			}
		}
	}
	
	public static void addItemToDatabase(int id, String name, boolean placeable, 
			String[] groups, Integer stackable, int refinedForm, ItemTotalDrops itemTotalDrops,
			int harvestTime, double baseValue, double beautyValue, 
			List<ProcessStep> itemActions, List<ProcessStep> properties,
			List<Vector3i> specBuildOffsets) {
		InventoryItem newItem = new InventoryItem(id, 0, name);
		allItemsById.put(id, newItem);
		itemNamesToIds.put(name, id);
		placeableBlock.put(id, placeable);
		
		boolean isBuilding = false;
		if (groups != null) {
			for (String group: groups) {
				group = group.trim();
				if (!group.isBlank()) {
					if (group.equals("Building")) {
						isBuilding = true;
					}
					if (!itemGroups.containsKey(group)) {
						itemGroups.put(group, new HashSet<>());
					}
					itemGroups.get(group).add(id);
					
					if (!groupNameById.containsKey(id)) {
						groupNameById.put(id, new HashSet<>());
					}
					groupNameById.get(id).add(group);
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
		//ItemTotalDrops.setItemExpectation(id);
		
		pickupTime.put(id, harvestTime);
		baseItemValue.put(id, baseValue);
		beautyItemValue.put(id, beautyValue);
		if (itemActions != null) {
			itemActionsById.put(id, itemActions);
			for (ProcessStep step: itemActions) {
				if (itemActionsNamed.get(step.stepType) == null) {
					itemActionsNamed.put(step.stepType, new HashSet<>());
				}
				itemActionsNamed.get(step.stepType).add(id);
			}
		}
		if (properties != null) {
			itemPropertiesById.put(id, properties);
			for (ProcessStep step: properties) {
				if (itemPropertiesNamed.get(step.stepType) == null) {
					itemPropertiesNamed.put(step.stepType, new HashSet<>());
				}
				itemPropertiesNamed.get(step.stepType).add(id);
			}
		}
		
		if (isBuilding) {
			if (specBuildOffsets != null && specBuildOffsets.size() > 0) {
				specialBuildingOffsets.put(id, specBuildOffsets);
				Pair<Vector3i> pointBounds = VecGridUtil.findCoordBounds(specBuildOffsets);
				Vector3i bounds = pointBounds.second.getSubtractedBy(pointBounds.first);
				buildingSizes.put(id, new Vector2i(bounds.x, bounds.y));
			}
			else {
				buildingSizes.put(id, new Vector2i(1, 1));
			}
		}
		
		//Adjust this id so that new ids will always never conflict with the current items
		GENERATED_BASE_ID = Math.max(GENERATED_BASE_ID, id); 
	}
	
	/**
	 * Note this method only updates id numbers. It does not create a new item.
	 * Use addItemToDatabase(...) or generateNewItem(String name) to create actual item entries.
	 */
	public static int generateIdNoNewItem() {
		GENERATED_BASE_ID++;
		return GENERATED_BASE_ID;
	}
	
	public static int generateNewItem(String name) {
		if (name.contains("<")) {
			throw new IllegalArgumentException("Attempted to generate a missing item with an angle bracket: " + name);
		}
		GENERATED_BASE_ID++;
		CustomLog.outPrintln("Generated item of name: " + name + ", id: " + GENERATED_BASE_ID);
		addItemToDatabase(GENERATED_BASE_ID, name, false, null, 15, ItemData.ITEM_EMPTY_ID, 
				null, 100, 0.0, 1.0, null, null, null);
		return GENERATED_BASE_ID;
	}
	
	/**
	 * @return all group names associated with this id. Null implies this item id is not within any group.
	 */
	public static Set<String> getGroupNameById(int id) {
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
		if (id == ITEM_EMPTY_ID) return true;
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
	
	public static double getBaseItemValue(Integer id) {
		if (!baseItemValue.containsKey(id)) {
			throw new IllegalArgumentException("Could not find item id: " + id);
		}
		return baseItemValue.get(id);
	}
	
	public static double getItemBeautyValue(Integer id) {
		if (!baseItemValue.containsKey(id)) {
			throw new IllegalArgumentException("Could not find item id: " + id);
		}
		return baseItemValue.get(id);
	}
	
	public static int getRefinedFormId(Integer id) {
		return refinedFormsById.containsKey(id) ? refinedFormsById.get(id) : ItemData.ITEM_EMPTY_ID;
	}
	
	public static List<ProcessStep> getItemActions(Integer id) {
		return itemActionsById.get(id);
	}

	public static List<ProcessStep> getItemProps(Integer id) {
		return itemPropertiesById.get(id);
	}
	
	public static double confirmItemActions(Integer id, String name) {
		List<ProcessStep> listActions = itemActionsById.get(id);
		for (ProcessStep action: listActions) {
			if (action.stepType.equals(name)) {
				return action.modifier;
			}
		}
		return -1;
	}
	
	public static Set<Integer> getItemsWithItemAction(String name) {
		return itemActionsNamed.get(name);
	}
	
	public static Set<Integer> getItemsWithItemProp(String name) {
		return itemPropertiesNamed.get(name);
	}
	public static boolean doesItemIdHaveProp(int id, String name) {
		Set<Integer> ids = ItemData.getItemsWithItemProp(name);
		return ids.contains(id);
	}
	
	public static int getTextureFromItemId(int id) {
		String itemName = getNameFromId(id);
		return VBOLoader.loadTexture(itemName, ConstantData.getItemTexturePath(itemName));
	}
	
	public static int getTextureFromItemId(String itemName) {
		return VBOLoader.loadTexture(itemName, ConstantData.getItemTexturePath(itemName));
	}
	
	public static List<String> itemsWithPartName(String part) {
		List<String> names = new ArrayList<>();
		for (String itemName : itemNamesToIds.keySet()) {
			if (itemName.contains(part)) {
				names.add(itemName);
			}
		}
		return names;
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
			building = new LocalBuilding(id, name, null, offsets, blockIds);
		}
		else {
			List<Integer> singleIdList = Collections.singletonList(id);
			building = new LocalBuilding(id, name, null, null, singleIdList);
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
	
	public static boolean isValidBuildingMaterial(int id) {
		return itemGroups.get("BuildingMaterial").contains(id);
	}
	
}
