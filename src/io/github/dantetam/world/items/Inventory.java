package io.github.dantetam.world.items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.toolbox.MathUti;
import io.github.dantetam.world.dataparse.ItemData;

public class Inventory {

	private List<InventoryItem> items;
	
	public Inventory() {
	
	}
	
	public Inventory(List<InventoryItem> listItems) {
		items = listItems;
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
		if (items == null) items = new ArrayList<>();
		int inventoryIndex = 0;
		int amountToAdd = addItem.quantity;
		int maxStack = ItemData.getMaxStackSize(addItem.itemId);
		while (true) {
			if (amountToAdd <= 0) {
				break;
			}
			if (inventoryIndex < items.size()) {
				InventoryItem invItem = items.get(inventoryIndex);
				if (invItem.itemId == addItem.itemId) {
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
			items.add(new InventoryItem(addItem.itemId, currentAdded, addItem.name));
		}
	}
	
	public int findItemCount(int itemId) {
		if (items == null) return 0;
		int sum = 0;
		for (InventoryItem item: items) {
			if (item.itemId == itemId) {
				sum += item.quantity;
			}
		}
		return sum;
	}
	
	public int findItemCountGroup(String groupName) {
		if (items == null) return 0;
		int sum = 0;
		Set<Integer> groupIds = ItemData.getGroupIds(groupName);
		for (InventoryItem item: items) {
			if (groupIds.contains(item.itemId)) {
				sum += item.quantity;
			}
		}
		return sum;
	}
	
	/**
	 * @param requiredItems A list of items with id and quantity.
	 * @return Two maps containing the items needed to complete the request that are not in this inventory
	 */
	public Object[] findRemainingItemsNeeded(List<InventoryItem> requiredItems) {
		//Tally up all required needed items
		Map<Integer, Integer> regularItemNeeds = new HashMap<>();
		Map<String, Integer> groupItemNeeds = new HashMap<>();
		for (InventoryItem requiredItem : requiredItems) {
			int reqId = requiredItem.itemId;
			String reqItemName = ItemData.getNameFromId(reqId);
			if (ItemData.isGroup(reqItemName)) {
				if (!groupItemNeeds.containsKey(reqItemName)) {
					groupItemNeeds.put(reqItemName, 0);
				}
				groupItemNeeds.put(reqItemName, groupItemNeeds.get(reqItemName) + requiredItem.quantity);
			}
			else {
				if (!regularItemNeeds.containsKey(reqId)) {
					regularItemNeeds.put(reqId, 0);
				}
				regularItemNeeds.put(reqId, regularItemNeeds.get(reqId) + requiredItem.quantity);
			}
		}
		
		List<InventoryItem> cloneInv = new ArrayList<>();
		//Clone the inventory and use items as necessary to reduce the demand count
		if (items != null) {
			for (InventoryItem invItem : items) {
				InventoryItem cloneInvItem = invItem.clone();
				cloneInv.add(cloneInvItem);
				int itemId = cloneInvItem.itemId;
				Set<String> candidateGroups = ItemData.getGroupNameById(itemId);
				if (candidateGroups != null) {
					for (String candidateGroup: candidateGroups) {
						if (candidateGroup != null && groupItemNeeds.containsKey(candidateGroup)) {
							int requiredQuantity = groupItemNeeds.get(candidateGroup);
							int subtract = Math.min(requiredQuantity, Math.max(0, cloneInvItem.quantity));
							cloneInvItem.quantity -= subtract;
							if (requiredQuantity - subtract > 0)
								groupItemNeeds.put(candidateGroup, requiredQuantity - subtract);
							else 
								groupItemNeeds.remove(candidateGroup);
						}
					}
				}
				if (regularItemNeeds.containsKey(itemId)) {
					int requiredQuantity = regularItemNeeds.get(itemId);
					int subtract = Math.min(requiredQuantity, Math.max(0, cloneInvItem.quantity));
					cloneInvItem.quantity -= subtract;
					if (requiredQuantity - subtract > 0)
						regularItemNeeds.put(itemId, requiredQuantity - subtract);
					else 
						regularItemNeeds.remove(itemId);
				}
			}
		}
		
		for (int itemIndex = items.size() - 1; itemIndex >= 0; itemIndex--) {
			InventoryItem item = items.get(itemIndex);
			if (item.quantity <= 0) {
				items.remove(itemIndex);
			}
		}
		
		return new Object[] {regularItemNeeds, groupItemNeeds, cloneInv};
	}
	
	public boolean hasItem(InventoryItem item) {
		List<InventoryItem> items = new ArrayList<>();
		items.add(item);
		return hasItems(items);
	}
	public boolean hasItems(List<InventoryItem> requiredItems) {
		if (items == null || requiredItems.size() > items.size()) return false;
		Object[] itemNeedData = findRemainingItemsNeeded(requiredItems);
		Map<Integer, Integer> regularItemNeeds = (Map) itemNeedData[0]; 
		Map<String, Integer> groupItemNeeds = (Map) itemNeedData[1];
		return regularItemNeeds.size() == 0 && groupItemNeeds.size() == 0;
	}
	
	public void subtractItem(InventoryItem item) {
		List<InventoryItem> items = new ArrayList<>();
		items.add(item);
		subtractItems(items);
	}
	public void subtractItems(List<InventoryItem> requiredItems) {
		Object[] itemNeedData = findRemainingItemsNeeded(requiredItems);
		Map<Integer, Integer> regularItemNeeds = (Map) itemNeedData[0]; 
		Map<String, Integer> groupItemNeeds = (Map) itemNeedData[1];
		List<InventoryItem> cloneInv = (List) itemNeedData[2];
		if (regularItemNeeds.size() == 0 && groupItemNeeds.size() == 0) {
			this.items = cloneInv;
			if (cloneInv.size() == 0) this.items = null;
		}
	}
	
	public List<InventoryItem> getItems() {
		return items;
	}
	
	public boolean canFitItems(List<InventoryItem> items) {
		return true;
	}
	
	//Pure utility wealth. See Society.java for the also relevant measure of "societal utility wealth",
	//which factors in the scarcity and needs of resources in a society.
	public double getTotalWealth() {
		if (this.items == null) return 0;
		double sumWealth = 0;
		for (InventoryItem item: this.items) {
			double wealth = ItemData.getBaseItemValue(item.itemId);
			sumWealth += wealth;
		}
		return sumWealth;
	}
	
	public int size() {
		if (items == null) return 0;
		return items.size();
	}
	
	public String toString() {
		String itemsList = "Inventory: [";
		if (items != null) {
			for (InventoryItem item: items) {
				itemsList += item.toString() + "; ";
			}
		}
		return itemsList + "]";
	}
	
	public int hashCode() {
		int h = 0;
        for (InventoryItem item: items) {
            h = 31 * h + item.name.hashCode() * item.quantity;
        }
	    return h;
	}
	
	public Map<String, Integer> toUniqueItemsMap() {
		Map<String, Integer> uniqueCounts = new HashMap<>();
		for (InventoryItem item: items) {
			MathUti.addNumMap(uniqueCounts, ItemData.getNameFromId(item.itemId), item.quantity);
		}
		return uniqueCounts;
	}
	
}
