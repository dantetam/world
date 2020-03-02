package io.github.dantetam.world.items;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
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
import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.WorldCsvParser;
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
	
	public Inventory(Collection<InventoryItem> listItems) {
		this();
		addItems(listItems);
	}
	
	public void addItems(Collection<InventoryItem> items) {
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
		List<InventoryItem> items = this.findItems(itemId, mainHuman, otherOwners);
		int sum = 0;
		for (InventoryItem item: items) {
			sum += item.quantity;
		}
		return sum;
	}
	
	public int findItemCountGroup(String groupName, LivingEntity mainHuman, Set<LivingEntity> otherOwners) {
		List<InventoryItem> items = this.findItemsGroup(groupName, mainHuman, otherOwners);
		int sum = 0;
		for (InventoryItem item: items) {
			sum += item.quantity;
		}
		return sum;
	}
	
	public List<InventoryItem> findItems(int itemId, LivingEntity mainHuman, Set<LivingEntity> otherOwners) {
		List<InventoryItem> validItems = new ArrayList<>();
		if (!(itemsMappedById.containsKey(itemId))) return validItems;
		List<InventoryItem> items = itemsMappedById.get(itemId);
		for (InventoryItem item: items) {
			if (item.beingHasAccessItem(mainHuman, otherOwners)) {
				validItems.add(item);
			}
		}
		return validItems;
	}
	
	public List<InventoryItem> findItemsGroup(String groupName, LivingEntity mainHuman, Set<LivingEntity> otherOwners) {
		List<InventoryItem> validItems = new ArrayList<>();
		if (itemsMappedById == null) return validItems;
		Set<Integer> groupItemIds = ItemData.getIdsFromNameOrGroup(groupName);
		for (Integer itemId: groupItemIds) {
			List<InventoryItem> items = itemsMappedById.get(itemId);
			for (InventoryItem item: items) {
				if (item.beingHasAccessItem(mainHuman, otherOwners)) {
					validItems.add(item);
				}
			}
		}
		return validItems;
	}
	
	/** 
	 * Find an algorithm that factors in quality, by choosing the items that have the lowest viable quality
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
		
	//Find an algorithm that factors in quality, by choosing the items that have the lowest viable quality
	/**
	 * @param requiredGroups A list of GroupItem with group names and quantity counts (split by quality).
	 * 		For now, the quality matches are strictly equal or better.
	 * 
	 * This has to be a much more complex constraint satisfaction problem (CSP), 
	 * because of the following case:
	 * 
	 * Consider a desire for items of group A, 5 good quality, and group B, 3 normal quality.
	 * Consider two items 
	 * (group A/B, 3) (group B, 5)
	 * 
	 * If a naive searching/greedy alg. picks group A/B, 3 to fill the desire for B,
	 * then there are no more items to fill the desire for item A. 
	 * A solution group A/B, 3 -> A and group B, 3 -> B clearly exists here.
	 * 
	 * @return Three items:
	 * 		a boolean for success;
	 * 	    a Map<InventoryItem, Integer> matching the items used to fulfill this request, maximum possible
	 * 		a List<GroupItem> of the remaining unfulfilled needs (beyond the best guess that is successful or not)
	 */
	public Object[] findRemainingGroupsNeeded(List<GroupItem> requiredGroupsOrig) {		
		List<GroupItem> reqGroups = new ArrayList<>();
		for (GroupItem reqItem: requiredGroupsOrig) {
			reqGroups.add(reqItem.clone());
		}
		
		//Map<GroupItem, Set<InventoryItem>> countedItems = new HashMap<>();
		
		//Clone the inventory and use items as necessary to reduce the demand count
		if (itemsMappedById != null) {
			Map<InventoryItem, Set<Integer>> ableSatisfyItemToNeedIndex = new HashMap<>();
			
			for (Entry<Integer, List<InventoryItem>> inventoryEntry: itemsMappedById.entrySet()) {
				//Sort the items in this inventory by quality DEscending
				int itemId = inventoryEntry.getKey();
				
				Set<String> allGroupsWithId = ItemData.getGroupNameById(itemId);
				
				if (this.currentSortedQualityMapId.containsKey(itemId) &&
						this.currentSortedQualityMapId.get(itemId)) {
					Collections.sort(inventoryEntry.getValue(), new Comparator<InventoryItem>() {
						public int compare(InventoryItem o1, InventoryItem o2) {
							return ItemQuality.getIndex(o2.quality) - ItemQuality.getIndex(o1.quality);
						}
					});
					this.currentSortedQualityMapId.put(itemId, true);
				}
				
				List<InventoryItem> itemsWithId = inventoryEntry.getValue();
				
				for (int groupReqIndex = 0; groupReqIndex < reqGroups.size(); groupReqIndex++) {
					GroupItem reqItem = reqGroups.get(groupReqIndex);
					if (reqItem.desiredCount > 0 && allGroupsWithId.contains(reqItem.group)) {
						for (InventoryItem invItem: itemsWithId) {
							if (invItem.quantity <= 0) continue;
							if (ItemQuality.equalOrBetter(reqItem.qualityMin, invItem.quality)) {
								MapUtil.insertNestedSetMap(ableSatisfyItemToNeedIndex, invItem.clone(), groupReqIndex);
							}
							else {
								break;
							}
						}
					}
				}
			}
			
			//A graph is contained within ableSatisfyItemToNeedIndex,
			//which contains every item mapped to every possible need that the item can fulfill.
			//Now, start a CSP backtracking process where the items that fulfill the least needs,
			//are "used" first to fulfill needs. 
			//Backtrack one decision if the current solution is impossible.
			//End either once a solution is found,
			//or there are no more satisfying relationships (no solution).
			
			List<ItemSatisfy> storedStackUsedNeeds = new ArrayList<>();
			Set<Entry<InventoryItem, GroupItem>> forbiddenSat = new HashSet<>();
			
			//Use this to hold a record of the best current fulfillment of the needs,
			//based on item counts and item type counts that were matched.
			//This result is important for returning a good guess of what more to get,
			//to fulfill the request fully.
			List<ItemSatisfy> bestPartialMatchTrack = null;
			
			int needsSatisfied = 0;
			
			while (true) { //ableSatisfyItemToNeedIndex.size() > 0
				
				CustomLog.outPrintln("----------------------");
				CustomLog.outPrintln("Needs: " + reqGroups);
				CustomLog.outPrintln("SatisfyRel: " + ableSatisfyItemToNeedIndex);
				
				Entry<InventoryItem, Set<Integer>> minEntry = null;
				for (Entry<InventoryItem, Set<Integer>> entry: ableSatisfyItemToNeedIndex.entrySet()) {
					if (minEntry == null || minEntry.getValue().size() < entry.getValue().size()) {
						//Find if this item fulfill an existing need with greater than zero need 
						//i.e. not already fulfilled.
						for (Integer groupIndex: entry.getValue()) {
							if (reqGroups.get(groupIndex).desiredCount > 0) {
								minEntry = entry;
								break;
							}
						}
					}
				}
				
				boolean foundMatching = false;
				
				if (minEntry != null) {
					InventoryItem usedItem = minEntry.getKey();
					for (int randomNeedIndex : minEntry.getValue()) {
						GroupItem need = reqGroups.get(randomNeedIndex);
						
						if (forbiddenSat.contains(new AbstractMap.SimpleEntry<>(usedItem, need))) {
							continue;
						}
						
						if (need.desiredCount > 0) {
							int use = Math.min(need.desiredCount, usedItem.quantity);
							CustomLog.outPrintln("Fulfill: " + need + "; " + usedItem + "; " + use);
							need.desiredCount -= use;
							usedItem.quantity -= use;
							
							ItemSatisfy chosenFulfill = new ItemSatisfy(usedItem, need, use);
							storedStackUsedNeeds.add(chosenFulfill);
							
							//Update bestPartialMatchTrack with the current longest result
							if (bestPartialMatchTrack == null || bestPartialMatchTrack.size() < storedStackUsedNeeds.size()) {
								//Use a clone because storedStackUsedNeeds can modify and add more to its sequence
								bestPartialMatchTrack = new ArrayList<>(storedStackUsedNeeds);
							}
							
							if (usedItem.quantity == 0) {
								ableSatisfyItemToNeedIndex.remove(usedItem);
							}
							if (need.desiredCount == 0) {
								needsSatisfied++;
								if (needsSatisfied == reqGroups.size()) { //Found a solution
									Map<InventoryItem, Integer> itemUsage = new HashMap<>();
									for (ItemSatisfy itemUse: storedStackUsedNeeds) {
										MapUtil.addNumMap(itemUsage, itemUse.itemUsed, itemUse.numUsed);
									}
									return new Object[] {true, itemUsage, new ArrayList<>()};
								}
							}
							
							foundMatching = true;
							break;
						}
					}
				}
				
				/*
				 * This is reached if there's no min entry, or the min entry was not satisfactory
				 * in finding a viable match.
				 */
				if (!foundMatching) {
					if (storedStackUsedNeeds.size() == 0) {
						//No solution, return the best partial match
						Map<InventoryItem, Integer> itemUsage = new HashMap<>();
						if (bestPartialMatchTrack != null) {
							for (ItemSatisfy itemUse: bestPartialMatchTrack) {
								MapUtil.addNumMap(itemUsage, itemUse.itemUsed, itemUse.numUsed);
								//Edit the copy of all the needs that need to be satisfied
								reqGroups.remove(itemUse.needSatisfied);
							}
						}
						return new Object[] {false, itemUsage, bestPartialMatchTrack};
					}
					else {
						//This item satisfaction is guaranteed to not lead to a solution
						//i.e. a sequence of item satisfactions [i_1, i_2, ..., i_N-1, i_N],
						//then a solution cannot exist with this sequence, iff we cannot expand
						//more item satisfactions from i_N.
						//We should backtrack to [...i_N-1], and mark i_N as impossible.
						
						//TODO: ^Needs proof
						
						ItemSatisfy backtrack = storedStackUsedNeeds.remove(storedStackUsedNeeds.size() - 1);
						backtrack.itemUsed.quantity += backtrack.numUsed;
						backtrack.needSatisfied.desiredCount += backtrack.numUsed;
						//CustomLog.outPrintln("Could not satisfy, bringing back: " + backtrack.itemUsed + "; " + 
								//backtrack.needSatisfied);
						needsSatisfied--;
						forbiddenSat.add(new AbstractMap.SimpleEntry<>(backtrack.itemUsed, backtrack.needSatisfied));
					}
				}
			}
		}
		
		//No solution because no items
		return new Object[] {false, new HashMap<>(), requiredGroupsOrig};
	}
	
	//Find an algorithm that factors in quality, by choosing the items that have the lowest viable quality
	/**
	 * @param requiredGroups A list of GroupItem with group names and quantity counts (split by quality).
	 * 		For now, the quality matches are strictly equal or better.
	 * 
	 * @return Three items:
	 * 		a boolean for success;
	 * 	    a Map<InventoryItem, Integer> matching the items used to fulfill this request, maximum possible
	 * 		a List<GroupItem> of the remaining unfulfilled needs (beyond the best guess that is successful or not)
	 *      Map<InventoryItem, Inventory> mapping items to their original inventories
	 */
	public static Object[] findRemainingGroupsNeeded(List<Inventory> inventories, 
			List<GroupItem> requiredGroupsOrig) {	
		List<GroupItem> remainingGroups = new ArrayList<>(requiredGroupsOrig);
		Map<InventoryItem, Integer> allUsedItems = new HashMap<>();
		Map<InventoryItem, Inventory> itemToInv = new HashMap<>(); //Top down inventory -> item model, keep track of where items are
		for (Inventory inventory: inventories) {
			Object[] singleInvResult = inventory.findRemainingGroupsNeeded(remainingGroups);
			boolean success = (boolean) singleInvResult[0];
			Map<InventoryItem, Integer> usedInvItems = (Map<InventoryItem, Integer>) singleInvResult[1];
			List<GroupItem> remainingNeeds = (List<GroupItem>) singleInvResult[2];
			
			MapUtil.addMapToMap(allUsedItems, usedInvItems);
			for (InventoryItem item: usedInvItems.keySet()) {
				itemToInv.put(item, inventory);
			}
			
			if (success) {
				return new Object[] {true, allUsedItems, new ArrayList<>(), itemToInv};
			}
			
			remainingGroups = remainingNeeds;
		}
		return new Object[] {false, allUsedItems, remainingGroups, itemToInv};
	}
	
	public static class ItemSatisfy { //Used in the group selection/search algorithm
		public InventoryItem itemUsed;
		public GroupItem needSatisfied;
		public int numUsed;
		public ItemSatisfy(InventoryItem itemUsed, GroupItem needSatisfied, int numUsed) {
			this.itemUsed = itemUsed;
			this.needSatisfied = needSatisfied;
			this.numUsed = numUsed;
		}
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
			removeItems(requestedItems);
			return requestedItems.keySet();
		}
		return null;
	}
	public Set<InventoryItem> subtractItemsGroups(List<GroupItem> requiredGroups) {
		Object[] itemNeedData = this.findRemainingGroupsNeeded(requiredGroups);
		Boolean foundItems = (Boolean) itemNeedData[0]; 
		Map<InventoryItem, Integer> requestedItems = (Map) itemNeedData[1];
		if (foundItems) {
			removeItems(requestedItems);
			return requestedItems.keySet();
		}
		return null;
	}
	public static Set<InventoryItem> subItemGroupMultiInv(List<Inventory> invs, List<GroupItem> requiredGroups) {
		Object[] itemNeedData = Inventory.findRemainingGroupsNeeded(invs, requiredGroups);
		Boolean foundItems = (Boolean) itemNeedData[0]; 
		Map<InventoryItem, Integer> requestedItems = (Map) itemNeedData[1];
		Map<InventoryItem, Inventory> itemToInv = (Map) itemNeedData[3];
		if (foundItems) {
			for (Entry<InventoryItem, Inventory> entry: itemToInv.entrySet()) {
				int numNeed = requestedItems.get(entry.getKey());
				entry.getValue().removeItem(entry.getKey(), numNeed);
			}
			return requestedItems.keySet();
		}
		return null;
	}
	
	public void removeItem(InventoryItem item, int amountSubtract) {
		item.quantity -= amountSubtract;
		if (item.quantity == 0) {
			//Check if item is actually in inventory, since this method is exposed public
			if (!this.itemsMappedById.containsKey(item.itemId)) {
				throw new IllegalArgumentException("Internal method in Inventory::removeItems requires " 
						+ "item id to be recorded in this inventory");
			}
			List<InventoryItem> itemsWithId = this.itemsMappedById.get(item.itemId);
			if (!itemsWithId.contains(item)) {
				throw new IllegalArgumentException("Internal method in Inventory::removeItems requires " 
						+ "exact items to be in this inventory");
			}
			itemsWithId.remove(item);
		}
	}
	public void removeItems(Map<InventoryItem, Integer> requestedItems) {
		for (Entry<InventoryItem, Integer> entry: requestedItems.entrySet()) {
			InventoryItem item = entry.getKey();
			Integer amountSubtract = entry.getValue();
			removeItem(item, amountSubtract);
		}
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
	
	public static void main(String[] args) {
		/**
		   This is to test the group search algorithm.
		 
		   Group A = Clothing
		   Group B = Armor
		  
		   Items in Group A and B: Cloth Shirt, Cloth Tunic
		   Items in Group B only: Iron Helmet
		  
		   The test case is looking for these needs: 
		   
		   A, 5, Q+
		   B, 2, Q
		   B, 1, Q++
		   
		   with an inventory containing the items:
		   
		   A/B, 2,  Q+
		   A/B, 3,  Q++
		   B,   4,  Q
		   B,   1,  Q++
		   
		   of which the solution should be
		   
		   
		   
		   The next case is needs:
		   
		   A, 5, Q
		   B, 6, Q
		       
		   with the items:
		   
		   A,   6,  Q
		   A/B, 6,  Q
		*/
		
		WorldCsvParser.init();
		
		List<GroupItem> groupNeeds = new ArrayList<>();
		groupNeeds.add(new GroupItem("Clothing", ItemQuality.GREAT, 5));
		groupNeeds.add(new GroupItem("Armor", ItemQuality.GOOD, 2));
		groupNeeds.add(new GroupItem("Armor", ItemQuality.LEGENDARY, 1));
		
		//Do not initialize items like this for normal gameplay,
		//use the ItemData.item, createItem... functions.
		Inventory inventory = new Inventory();
		inventory.addItem(new InventoryItem(ItemData.getIdFromName("Cloth Shirt"), 2, ItemQuality.GREAT, "Cloth Shirt"));
		inventory.addItem(new InventoryItem(ItemData.getIdFromName("Cloth Tunic"), 3, ItemQuality.LEGENDARY, "Cloth Tunic"));
		inventory.addItem(new InventoryItem(ItemData.getIdFromName("Iron Helmet"), 4, ItemQuality.GOOD, "Iron Helmet"));
		inventory.addItem(new InventoryItem(ItemData.getIdFromName("Iron Helmet"), 1, ItemQuality.LEGENDARY, "Iron Helmet"));
	
		Object[] results = inventory.findRemainingGroupsNeeded(groupNeeds);
		Boolean found = (Boolean) results[0];
		Map<InventoryItem, Integer> chosenItems = (Map<InventoryItem, Integer>) results[1];
		
		System.err.println("Found Needs Final: " + found);
		System.err.println(chosenItems);
		
		System.err.println("##################################");
		
		groupNeeds = new ArrayList<>();
		groupNeeds.add(new GroupItem("Clothing", ItemQuality.GOOD, 5));
		groupNeeds.add(new GroupItem("Armor", ItemQuality.GOOD, 6));
		
		//Do not initialize items like this for normal gameplay,
		//use the ItemData.item, createItem... functions.
		inventory = new Inventory();
		inventory.addItem(new InventoryItem(ItemData.getIdFromName("Cloth Shirt"), 6, ItemQuality.GOOD, "Cloth Shirt"));
		inventory.addItem(new InventoryItem(ItemData.getIdFromName("Iron Helmet"), 6, ItemQuality.GOOD, "Iron Helmet"));
		
		results = inventory.findRemainingGroupsNeeded(groupNeeds);
		found = (Boolean) results[0];
		chosenItems = (Map<InventoryItem, Integer>) results[1];
		
		System.err.println("Found Needs Final: " + found);
		System.err.println(chosenItems);
		
		System.err.println("##################################");
		
		groupNeeds = new ArrayList<>();
		groupNeeds.add(new GroupItem("Clothing", ItemQuality.GOOD, 2));
		groupNeeds.add(new GroupItem("Armor", ItemQuality.GOOD, 2));
		
		//Do not initialize items like this for normal gameplay,
		//use the ItemData.item, createItem... functions.
		inventory = new Inventory();
		inventory.addItem(new InventoryItem(ItemData.getIdFromName("Iron Helmet"), 6, ItemQuality.GOOD, "Iron Helmet"));
		
		results = inventory.findRemainingGroupsNeeded(groupNeeds);
		found = (Boolean) results[0];
		chosenItems = (Map<InventoryItem, Integer>) results[1];
		
		System.err.println("Found Needs Final: " + found);
		System.err.println(chosenItems);
	}
	
}
