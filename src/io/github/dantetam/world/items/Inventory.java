package io.github.dantetam.world.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Iterator;
import java.util.Comparator;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.items.InventoryItem.ItemQuality;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;

public class Inventory {

	private Map<Integer, List<InventoryItem>> itemsMappedById; 
	//Assumed that the owner of this inventory has access to all items in this inventory
	
	private Map<Integer, Boolean> currentSortedQualityMapId; 
	//Keep track of when it is necessary to sort item by quality
	
	
	public Inventory() {
		this.itemsMappedById = new HashMap<>();
		this.currentSortedQualityMapId = new HashMap<>();
	}
	
	public Inventory(List<InventoryItem> listItems) {
		this();
		addItems(listItems);
	}
	
	public void addItems(List<InventoryItem> items) {
		for (InventoryItem item: items) {
			if (item != null) {
				addItem(item);
			}
		}
	}
	public void addItem(InventoryItem addItem) {
		if (addItem == null) return; //For the case where an item drop returns nothing
		
		int amountToAdd = addItem.quantity;
		int maxStack = ItemData.getMaxStackSize(addItem.itemId);
		
		if (!itemsMappedById.containsKey(addItem.itemId)) {
			itemsMappedById.put(addItem.itemId, new ArrayList<>());
		}
		List<InventoryItem> itemsWithId = itemsMappedById.get(addItem.itemId);
		
		int inventoryIndex = 0;
		while (true) {
			if (amountToAdd <= 0) {
				break;
			}
			if (inventoryIndex < itemsWithId.size()) {
				InventoryItem invItem = itemsWithId.get(inventoryIndex);
				if (invItem.quality == addItem.quality) {
					int currentAdded = Math.min(maxStack - invItem.quantity, amountToAdd);
					amountToAdd -= currentAdded;
					invItem.quantity += currentAdded;
				}
				inventoryIndex++;
			}
			else
				break;
		}
		while (amountToAdd > 0) {
			int currentAdded = Math.min(maxStack, amountToAdd);
			amountToAdd -= currentAdded;
			InventoryItem item = new InventoryItem(addItem.itemId, currentAdded, addItem.quality, addItem.name);
			itemsWithId.add(item);
		}
		
		this.currentSortedQualityMapId.put(addItem.itemId, false);
	}
	
	public int findItemCount(int itemId) {
		return findItemCount(itemId, null, null);
	}
	
	public int findItemCountGroup(String groupName) {
		return findItemCountGroup(groupName, null, null);
	}
	
	public int findItemCount(int itemId, LivingEntity mainHuman, Set<LivingEntity> otherOwners) {
		if (!(itemsMappedById.containsKey(itemId))) return 0;
		List<InventoryItem> items = itemsMappedById.get(itemId);
		int sum = 0;
		for (InventoryItem item: items) {
			if (item.beingHasAccessItem(mainHuman, otherOwners)) {
				sum += item.quantity;
			}
		}
		return sum;
	}
	
	public int findItemCountGroup(String groupName, LivingEntity mainHuman, Set<LivingEntity> otherOwners) {
		if (itemsMappedById == null) return 0;
		int sum = 0;
		Set<Integer> groupItemIds = ItemData.getIdsFromNameOrGroup(groupName);
			for (Integer itemId: groupItemIds) {
			List<InventoryItem> items = itemsMappedById.get(itemId);
			for (InventoryItem item: items) {
				if (item.beingHasAccessItem(mainHuman, otherOwners)) {
					sum += item.quantity;
				}
			}
		}
		return sum;
	}
	
	//Find an algorithm that factors in quality, by choosing the items that have the lowest viable quality
	/**
	 * @param requiredItems A list of items with id and quantity.
	 * @return Two maps containing the items needed to complete the request that are not in this inventory
	 *  	The inventory items that can be used to fulfill this request, if possible
	 */
	public Object[] findRemainingItemsNeeded(List<InventoryItem> requiredItems) {		
		List<InventoryItem> neededItems = new ArrayList<>();
		for (InventoryItem reqItem: requiredItems) {
			neededItems.add(reqItem.clone());
		}
		
		Collections.sort(neededItems, new Comparator<InventoryItem>() {
			public int compare(InventoryItem o1, InventoryItem o2) {
				return ItemQuality.getIndex(o2.quality) - ItemQuality.getIndex(o1.quality);
			}
		});
		
		Map<InventoryItem, Integer> countedItems = new HashMap<>();
		
		//Clone the inventory and use items as necessary to reduce the demand count
		if (itemsMappedById != null) {
			for (Entry<Integer, List<InventoryItem>> inventoryEntry: itemsMappedById.entrySet()) {
				//Sort the items in this inventory by quality ascending
				int itemId = inventoryEntry.getKey();
				if (this.currentSortedQualityMapId.containsKey(itemId) &&
						this.currentSortedQualityMapId.get(itemId)) {
					Collections.sort(inventoryEntry.getValue(), new Comparator<InventoryItem>() {
						public int compare(InventoryItem o1, InventoryItem o2) {
							return ItemQuality.getIndex(o1.quality) - ItemQuality.getIndex(o2.quality);
						}
					});
					this.currentSortedQualityMapId.put(itemId, true);
				}
				
				List<InventoryItem> itemsWithId = inventoryEntry.getValue();
				
				Iterator<InventoryItem> itemIter = neededItems.iterator();
				while (itemIter.hasNext()) {
					InventoryItem neededItem = itemIter.next();
					if (neededItem.itemId == itemId) {
						for (InventoryItem haveItem: itemsWithId) {
							if (ItemQuality.equalOrBetter(neededItem.quality, haveItem.quality)) {
								int amountToTake = Math.min(neededItem.quantity, haveItem.quantity);
								countedItems.put(neededItem, amountToTake);
								neededItem.quantity -= amountToTake;
								if (neededItem.quantity <= 0) {
									itemIter.remove();
									break;
								}
							}
							else {
								continue;
							}
						}
					}
				}
			}
		}
		
		return new Object[] {neededItems.size() != 0, countedItems, neededItems};
	}
	
	public boolean hasItem(InventoryItem item) {
		List<InventoryItem> items = new ArrayList<>();
		items.add(item);
		return hasItems(items);
	}
	public boolean hasItems(List<InventoryItem> requiredItems) {
		if (itemsMappedById == null) return false;
		Object[] itemNeedData = findRemainingItemsNeeded(requiredItems);
		Boolean foundItems = (Boolean) itemNeedData[0]; 
		return foundItems;
	}
	
	/*
	//Find the items with 
	public List<InventoryItem> subtractItem(String groupOrItemName, int count) {
		if (this.findItemCountGroup(groupOrItemName) >= count) {
			if (items == null) return null;
			
			List<InventoryItem> foundItems = new ArrayList<>();
			Set<Integer> groupIds = ItemData.getIdsFromNameOrGroup(groupOrItemName);
			for (InventoryItem item: items) {
				if (groupIds.contains(item.itemId)) {
					foundItems.add(item);
				}
			}
			return foundItems;
		}
		else {
			return null;
		}
	}
	*/
	public Set<InventoryItem> subtractItem(InventoryItem item) {
		List<InventoryItem> items = new ArrayList<>();
		items.add(item);
		return subtractItems(items);
	}
	public Set<InventoryItem> subtractItems(List<InventoryItem> requiredItems) {
		Object[] itemNeedData = findRemainingItemsNeeded(requiredItems);
		Boolean foundItems = (Boolean) itemNeedData[0]; 
		Map<InventoryItem, Integer> requestedItems = (Map) itemNeedData[1];
		if (foundItems) {
			for (Entry<InventoryItem, Integer> entry: requestedItems.entrySet()) {
				InventoryItem item = entry.getKey();
				Integer amountSubtract = entry.getValue();
				item.quantity -= amountSubtract;
				if (item.quantity == 0) {
					List<InventoryItem> itemsWithId = this.itemsMappedById.get(item.itemId);
					itemsWithId.remove(item);
				}
			}
			return requestedItems.keySet();
		}
		return null;
	}
	
	public List<InventoryItem> getItems() {
		if (itemsMappedById == null) return new ArrayList<>();
		List<InventoryItem> items = new ArrayList<>();
		this.iterator().forEachRemaining(items::add);
		return items;
	}
	
	public boolean canFitItems(List<InventoryItem> items) {
		return true;
	}
	
	//Pure utility wealth. See Society.java for the also relevant measure of "societal utility wealth",
	//which factors in the scarcity and needs of resources in a society.
	public double getTotalWealth() {
		if (this.itemsMappedById == null) return 0;
		double sumWealth = 0;
		for (Iterator<InventoryItem> itemIter = this.iterator(); itemIter.hasNext(); ) {
			InventoryItem item = itemIter.next();
			double wealth = ItemData.getBaseItemValue(item.itemId);
			sumWealth += wealth * item.quantity;
		}
		return sumWealth;
	}
	
	public int size() {
		if (itemsMappedById == null) return 0;
		int sum = 0;
		for (Entry<Integer, List<InventoryItem>> entry: itemsMappedById.entrySet()) {
			sum += entry.getValue().size();
		}
		return sum;
	}
	
	public Iterator<InventoryItem> iterator() {
		return new Iterator<InventoryItem>() {

			Iterator<Integer> keyIter = itemsMappedById.keySet().iterator();
			Integer currentItemId = keyIter.hasNext() ? keyIter.next() : null; //For the case of empty inventory
			int index = 0;
			
			@Override
			public boolean hasNext() {
				// TODO Auto-generated method stub
				return !(currentItemId == null || 
						(itemsMappedById.get(currentItemId).size() >= index && !keyIter.hasNext()));
			}
	
			@Override
			public InventoryItem next() {
				List<InventoryItem> curList = itemsMappedById.get(currentItemId);
				InventoryItem item = curList.get(index);
				
				index++;
				if (index >= curList.size()) {
					index = 0;
					currentItemId = keyIter.next();
				}
				
				return item;
			}
		
		};
	}
	
	public String toString() {
		String itemsList = "Inventory: [";
		if (itemsMappedById != null) {
			for (Entry<Integer, List<InventoryItem>> entry: itemsMappedById.entrySet()) {
				for (InventoryItem item: entry.getValue()) {
					itemsList += item.toString() + "; ";
				}
			}
		}
		return itemsList + "]";
	}
	
	public int hashCode() {
		int h = 0;
        for (Entry<Integer, List<InventoryItem>> entry: itemsMappedById.entrySet()) {
			for (InventoryItem item: entry.getValue()) {
				h = 31 * h + item.name.hashCode() * item.quantity;
			}
		}
	    return h;
	}
	
	public Map<String, Integer> toUniqueItemsMap() {
		Map<String, Integer> uniqueCounts = new HashMap<>();
		
		for (Iterator<InventoryItem> itemIter = this.iterator(); itemIter.hasNext(); ) {
			InventoryItem item = itemIter.next();
			MapUtil.addNumMap(uniqueCounts, ItemData.getNameFromId(item.itemId), item.quantity);
		}
		
		return uniqueCounts;
	}
	
}
