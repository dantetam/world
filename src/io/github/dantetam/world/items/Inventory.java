package io.github.dantetam.world.items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.dantetam.world.dataparse.ItemData;

public class Inventory {

	private List<InventoryItem> items;
	
	public Inventory() {
		items = new ArrayList<>();
	}
	
	public void addItem(InventoryItem addItem) {
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
		for (InventoryItem invItem : items) {
			InventoryItem cloneInvItem = invItem.clone();
			cloneInv.add(cloneInvItem);
			int itemId = cloneInvItem.itemId;
			String candidateGroup = ItemData.getGroupNameById(itemId);
			if (candidateGroup != null && groupItemNeeds.containsKey(candidateGroup)) {
				int requiredQuantity = groupItemNeeds.get(candidateGroup);
				int subtract = Math.min(requiredQuantity, Math.max(0, cloneInvItem.quantity));
				cloneInvItem.quantity -= subtract;
				if (requiredQuantity - subtract > 0)
					groupItemNeeds.put(candidateGroup, requiredQuantity - subtract);
				else 
					groupItemNeeds.remove(candidateGroup);
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
		
		return new Object[] {regularItemNeeds, groupItemNeeds, cloneInv};
	}
	
	public boolean hasItems(List<InventoryItem> requiredItems) {
		Object[] itemNeedData = findRemainingItemsNeeded(requiredItems);
		Map<Integer, Integer> regularItemNeeds = (Map) itemNeedData[0]; 
		Map<String, Integer> groupItemNeeds = (Map) itemNeedData[1];
		return regularItemNeeds.size() == 0 && groupItemNeeds.size() == 0;
	}
	
	public void subtractItems(List<InventoryItem> requiredItems) {
		Object[] itemNeedData = findRemainingItemsNeeded(requiredItems);
		Map<Integer, Integer> regularItemNeeds = (Map) itemNeedData[0]; 
		Map<String, Integer> groupItemNeeds = (Map) itemNeedData[1];
		List<InventoryItem> cloneInv = (List) itemNeedData[2];
		if (regularItemNeeds.size() == 0 && groupItemNeeds.size() == 0) {
			this.items = cloneInv;
		}
	}
	
	public List<InventoryItem> getItems() {
		return items;
	}
	
}
