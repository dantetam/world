package io.github.dantetam.world.civilization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.github.dantetam.toolbox.MathUti;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ItemTotalDrops;
import io.github.dantetam.world.dataparse.ProcessData;
import io.github.dantetam.world.grid.LocalBuilding;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.Process;
import io.github.dantetam.world.process.Process.ProcessStep;

public class Society {

	public LocalGrid grid;
	private List<Human> inhabitants;
	public Vector3i societyCenter;
	
	public Society(LocalGrid grid) {
		this.grid = grid;
		inhabitants = new ArrayList<>();
	}
	
	public void addPerson(Human person) {
		inhabitants.add(person);
	}
	
	public List<Human> getAllPeople() {
		return inhabitants;
	}
	
	/**
	 * 
	 * @param allItemsUtility A mapping of all items linked to their societal utility
	 * @param human The human in question who has access to resources, buildings, etc.
	 * @param desiredNumProcess The number of processes returned in this search
	 * @param desiredItems The restriction to only search for processes 
	 * @return
	 */
	public Map<Process, Double> prioritizeProcesses(Map<Integer, Double> allItemsUtility, 
			Human human, int desiredNumProcess, Set<Integer> desiredItems) {
		Map<Integer, Double> sortedUtility = MathUti.getSortedMapByValueDesc(allItemsUtility);
		Object[] sortedKeys = sortedUtility.keySet().toArray();
		
		//System.out.println("Ranked items: ###############");
		//for (Entry<Integer, Double> entry: sortedUtility.entrySet()) {
			//System.out.println(ItemData.getNameFromId(entry.getKey()) + "; ranking: " + entry.getValue());
		//}
		
		Map<Process, Double> processByUtil = new HashMap<>();
		int i = 0;
		while (i < sortedKeys.length) { //processByUtil.size() < desiredNumProcess
			Integer itemId = (Integer) sortedKeys[i];
			Map<Process, Double> bestProcesses = findBestProcess(allItemsUtility, human, itemId);
			
			if (bestProcesses != null) {
				if (desiredItems == null || desiredItems.contains(itemId)) {
					//processByUtil.put(bestProcess, sortedUtility.get(itemId));
					for (Entry<Process, Double> entry: bestProcesses.entrySet()) {
						MathUti.insertKeepMaxMap(processByUtil, entry.getKey(), entry.getValue());
					}
				}
			}
			i++;
		}

		processByUtil = MathUti.getSortedMapByValueDesc(processByUtil);
		
		System.out.println("Ranked processes: #####");
		for (Entry<Process, Double> entry: processByUtil.entrySet()) {
			System.out.println(entry.getKey().name + "; ranking: " + entry.getValue());
		}
		
		Map<Process, Double> processByPercentage = MathUti.getNormalizedMap(processByUtil);
		return processByPercentage;
	}
	
	/**
	 * Return the first process at the highest level (closest to final product),
	 * which is possible to complete by this human, by just crafting and collecting raw resources.
	 * @param human The human in question who has access to resources, buildings, etc.
	 */
	public Map<Process, Double> findBestProcess(Map<Integer, Double> allItemsUtility, Human human, int outputItemId) {
		Set<Integer> visitedItemIds = new HashSet<>();
		Set<Integer> fringe = new HashSet<>();
		fringe.add(outputItemId);
		
		Map<Process, Double> bestProcesses = new HashMap<>();
		
		Map<Integer, Double> rawResRarity = findRawResourcesRarity(human);
		//System.out.println("raw resource rarity: ");
		//System.out.println(rawResRarity);
		
		//System.out.println("_______________________________________" + ItemData.getNameFromId(outputItemId));
		
		while (fringe.size() > 0) {
			Set<Integer> newFringe = new HashSet<>();
			for (Integer fringeId: fringe) {
				
				//System.out.println("Process for base item >>>: " + ItemData.getNameFromId(fringeId));
				
				List<Process> processes = ProcessData.getProcessesByOutput(fringeId);
				for (Process process: processes) {
					List<InventoryItem> inputs = new ArrayList<>();
					
					if (process.requiredBuildNameOrGroup != null) {
						inputs.add(ItemData.item(process.requiredBuildNameOrGroup, 1));
					}
					if (process.requiredTileNameOrGroup != null) {
						//System.out.println(ItemData.item(process.requiredTileNameOrGroup, 1) + ":processed->" + process.requiredTileNameOrGroup + "<<<<");
						inputs.add(ItemData.item(process.requiredTileNameOrGroup, 1));
					}
					for (InventoryItem input: process.inputItems) {
						inputs.add(input);
					}
					
					//System.out.println("Looking at process: " + process.toString());
					
					for (InventoryItem input: inputs) {
						double util = allItemsUtility.containsKey(input.itemId) ? 
								allItemsUtility.get(input.itemId) : 0;
						if (canCompleteProcess(process, rawResRarity)) {
							MathUti.insertKeepMaxMap(bestProcesses, process.clone(), util);
							//System.out.println("COMPLETE process: " + process.name + " " + util);
						}
						else {
							//System.out.println("Could not complete process: " + process);
						}
						//if (util > 0)
							//System.out.println("Found utility for base item: " + util);
						//else 
							//System.out.println("terminal");
						if (!visitedItemIds.contains(input.itemId)) {
							if (util > 0)
								newFringe.add(input.itemId);
								//System.out.println("Expanding from " + ItemData.getNameFromId(fringeId) + " -----> " + ItemData.getNameFromId(input.itemId));
						}
					}
				}
				visitedItemIds.add(fringeId);
			}	
			fringe = newFringe;
		}
		
		bestProcesses = MathUti.getSortedMapByValue(bestProcesses);
		return bestProcesses;
	}
	
	//Return the necessary items that are needed for a process
	public boolean canCompleteProcess(Process process, Map<Integer, Double> rawResRarity) {
		//Check for resources
		List<InventoryItem> inputs = process.inputItems;
		for (InventoryItem input: inputs) {
			if (!rawResRarity.containsKey(input.itemId) || rawResRarity.get(input.itemId) == 0) {
				//System.out.println("No input");
				return false;
			}
		}
		
		//Check for buildings
		if (process.requiredBuildNameOrGroup != null) {
			int buildingId = ItemData.getIdFromName(process.requiredBuildNameOrGroup);
			if (!rawResRarity.containsKey(buildingId) || rawResRarity.get(buildingId) == 0) {
				//System.out.println("No req building");
				return false;
			}
		}
		
		if (process.requiredTileNameOrGroup != null) {
			int tileHarvestId = ItemData.getIdFromName(process.requiredTileNameOrGroup);
			if (!rawResRarity.containsKey(tileHarvestId) || rawResRarity.get(tileHarvestId) == 0) {
				//System.out.println("No required tile");
				return false;
			}
		}
			
		//TODO: Check for required location and/or site
		return true;
	}
	
	/**
	 * @return A mapping of every item in the world available to its calcuated utility.
	 */
	public Map<Integer, Double> findCompleteUtilityAllItems(Human human) {
		Map<Integer, Double> allRarity = findAllAvailableResourceRarity(human);
		Map<Integer, Double> economicRarityMap = findAdjEconomicRarity();
		Set<Integer> availableItemIds = economicRarityMap.keySet();
		
		Map<String, Double> needsIntensity = findAllNeedsIntensity();
		
		Map<String, Double> needWeights = new HashMap<>();
		needWeights.put("Eat", 12.0);
		needWeights.put("Rest", 3.0);
		needWeights.put("Shelter", 1.0);
		
		//Used to normalize the values and determine 
		Map<String, Double> totalNeedsUtility = new HashMap<>(); 
		
		Map<Integer, Double> finalOutputUtility = new HashMap<>();
		
		Map<Integer, Double> combinedRarityMap = new HashMap<>();
		
		for (int itemId : availableItemIds) {
			double itemRarity = allRarity.containsKey(itemId) ? new Double(Math.log10(allRarity.get(itemId))) : 0;
			double economicRarity = new Double(Math.log10(economicRarityMap.get(itemId)));
			
			combinedRarityMap.put(itemId, (itemRarity + economicRarity) / 2.0);
			
			Function<Entry<String, Double>, Double> utilCalcBalance = e -> {
				Double intensity = needsIntensity.get(e.getKey());
				if (intensity == null) {
					intensity = new Double(0.5);
				}
				
				double needWeight = needWeights.containsKey(e.getKey()) ? needWeights.get(e.getKey()) : 1.0;
				
				double tempMultiplier = needWeights.containsKey("Wealth") ? needWeights.get("Wealth") : 0.5;
				
				//return (e.getValue() * intensity * needWeight) - (itemRarity * economicRarity);
				return tempMultiplier * e.getValue() * needWeight;
			};
			
			//Develop a basic utility value: item use * need for item use / rareness
			Map<String, Double> needsUtilityFromItems = findUtilityByNeed(itemId);
			needsUtilityFromItems.entrySet().stream().collect(Collectors.toMap(Entry::getKey,
					utilCalcBalance
					));
			
			double sumWeightsUtility = 0;
			
			for (Entry<String, Double> entry: needsUtilityFromItems.entrySet()) {
				MathUti.addNumMap(totalNeedsUtility, entry.getKey(), entry.getValue());
				sumWeightsUtility += entry.getValue();
			}
			
			finalOutputUtility.put(itemId, sumWeightsUtility);
			
			/*
			Map<String, Double> needsUtilityFromItems = findUtilityByNeed(itemId);
			needsUtilityFromItems.entrySet().stream().collect(Collectors.toMap(Entry::getKey,
					e -> e.getValue() * needsIntensity.get(e.getKey()) / itemRarity
					));
			*/
		}
		
		Map<Integer, Double> propogatedFinalUtil = backpropUtilToComponents(finalOutputUtility, availableItemIds);
		propogatedFinalUtil = MathUti.getSortedMapByValue(propogatedFinalUtil);
		
		//Map<String, Double> sortedNeedUtility = MathUti.getSortedMapByValue(totalNeedsUtility); 
		
		return propogatedFinalUtil;
	}
	
	/**
	 * Directly count the physical rarity and presence of all items available on the map
	 */
	private Map<Integer, Double> findRawResourcesRarity(Human human) {
		Map<Integer, Double> itemRarity = new HashMap<>();
		for (int r = 0; r < grid.rows; r++) {
			for (int c = 0; c < grid.cols; c++) {
				int startHeight = grid.findLowestEmptyHeight(r, c) - 1;
				for (int h = startHeight; h >= startHeight - 5; h--) {
					LocalTile tile = grid.getTile(new Vector3i(r, c, h));
					if (tile == null) continue;
					if (tile.itemsOnFloor.getItems() != null) {
						List<InventoryItem> items = tile.itemsOnFloor.getItems();
						for (InventoryItem item: items) {
							if (item.currentUser == null || human == null || item.currentUser.equals(human)) {
								int id = item.itemId;
								int num = item.quantity;
								MathUti.addNumMap(itemRarity, id, (double) num);
							}
						}
					}
					if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID) {
						MathUti.addNumMap(itemRarity, tile.tileBlockId, 1.0);
					}
					/*
					if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID) {
						ItemTotalDrops drops = ItemData.getOnBlockItemDrops(tile.tileBlockId);
						Map<Integer, Double> itemExpectations = drops.itemExpectation();
						for (Entry<Integer, Double> entry: itemExpectations.entrySet()) {
							MathUti.addNumMap(itemRarity, entry.getKey(), entry.getValue());
						}
					}
					*/
				}
			}
		}
		for (LocalBuilding building: grid.getAllBuildings()) {
			if (human == null || building.owner == null || building.owner.equals(human)) {
				MathUti.addNumMap(itemRarity, building.buildingId, 1.0);
				for (int itemId: building.buildingBlockIds) {
					ItemTotalDrops drops = ItemData.getOnBlockItemDrops(itemId);
					Map<Integer, Double> itemExpectations = drops.itemExpectation();
					for (Entry<Integer, Double> entry: itemExpectations.entrySet()) {
						MathUti.addNumMap(itemRarity, entry.getKey(), entry.getValue());
					}
				}
			}
		}
		if (human == null) {
			for (Human everyHuman: inhabitants) {
				List<InventoryItem> items = everyHuman.inventory.getItems();
				for (InventoryItem item: items) {
					MathUti.addNumMap(itemRarity, item.itemId, (double) item.quantity);
				}
			}
		}
		else {
			List<InventoryItem> items = human.inventory.getItems();
			for (InventoryItem item: items) {
				MathUti.addNumMap(itemRarity, item.itemId, (double) item.quantity);
			}
		}
		return itemRarity;
	}
	
	/**
	 * Directly count the adjusted economic rarity of all items on the map i.e.
	 * the inherent value of items on a 'free market'. This is useful for not having
	 * too many people produce the same resource, i.e. if wooden walls are the most
	 * profitable good when there are none, but 
	 */
	private Map<Integer, Double> findAdjEconomicRarity() {
		Map<Integer, Double> itemRarity = this.findAllAvailableResourceRarity(null);
		/*
		for (int r = 0; r < grid.rows; r++) {
			for (int c = 0; c < grid.cols; c++) {
				int startHeight = grid.findLowestEmptyHeight(r, c) - 1;
				for (int h = startHeight; h >= startHeight - 5; h--) {
					LocalTile tile = grid.getTile(new Vector3i(r, c, h));
					if (tile == null) continue;
					//
				}
			}
		}
		*/
		for (LocalBuilding building: grid.getAllBuildings()) {
			for (int itemId: building.buildingBlockIds) {
				ItemTotalDrops drops = ItemData.getOnBlockItemDrops(itemId);
				Map<Integer, Double> itemExpectations = drops.itemExpectation();
				for (Entry<Integer, Double> entry: itemExpectations.entrySet()) {
					MathUti.addNumMap(itemRarity, entry.getKey(), entry.getValue());
				}
			}
		}
		for (Human everyHuman: inhabitants) {
			List<InventoryItem> items = everyHuman.inventory.getItems();
			for (InventoryItem item: items) {
				MathUti.addNumMap(itemRarity, item.itemId, (double) item.quantity);
			}
			if (everyHuman.processProgress != null) {
				ItemTotalDrops drops = everyHuman.processProgress.outputItems;
				if (drops != null) {
					Map<Integer, Double> itemExpectations = drops.itemExpectation();
					for (Entry<Integer, Double> entry: itemExpectations.entrySet()) {
						MathUti.addNumMap(itemRarity, entry.getKey(), entry.getValue() * 5);
					}
				}
			}
		}
		return itemRarity;
	}
	
	/**
	 * Using raw resources, figure out every possible item that could be generated from destroying,
	 * crafting, or otherwise processing the available items. Return all possible items that this
	 * society could create, and their associated rareness.
	 */
	private Map<Integer, Double> findAllAvailableResourceRarity(Human human) {
		Set<Integer> visitedItemIds = new HashSet<>();
		Set<Integer> fringe = new HashSet<>();
		Map<Integer, Double> rawResources = findRawResourcesRarity(human);
		for (Integer itemId: rawResources.keySet()) {
			fringe.add(itemId);
		}
		
		Map<Integer, Double> itemRarity = new HashMap<>(rawResources);
		//Expand all of the item process chains until this map does not change size
		//A level order traversal (in terms of the expanding set)
		while (fringe.size() > 0) {
			Set<Integer> newFringe = new HashSet<>();
			for (Integer itemId: fringe) {
				List<Process> inputProcessses = ProcessData.getProcessesByInput(itemId);
				
				//Collect all possible items that can come from breaking or crafting with itemId
				List<ItemTotalDrops> allItemDrops = new ArrayList<>();
				ItemTotalDrops onBlockDropExp = ItemData.getOnBlockItemDrops(itemId);
				if (onBlockDropExp != null && ItemData.isPlaceable(itemId)) {
					allItemDrops.add(onBlockDropExp);
				}
				for (Process process: inputProcessses) {
					allItemDrops.add(process.outputItems);
				}
				
				for (ItemTotalDrops itemDrop: allItemDrops) {
					Map<Integer, Double> itemExpectations = itemDrop.itemExpectation();
					for (Entry<Integer, Double> entry: itemExpectations.entrySet()) {
						int nextItemId = entry.getKey();
						//Expand the item's process chain if necessary, otherwise just add to a running count
						if (!visitedItemIds.contains(nextItemId)) {
							newFringe.add(nextItemId);
						}
						//System.out.println(ItemData.getNameFromId(itemId) + " ---expand---> " + ItemData.getNameFromId(nextItemId));
						MathUti.addNumMap(itemRarity, nextItemId, entry.getValue());
					}
				}
				
				visitedItemIds.add(itemId);
			}
			fringe = newFringe;
		}
		
		itemRarity.entrySet().stream().collect(Collectors.toMap(Entry::getKey,
				e -> e.getValue() * ItemData.getBaseItemValue(e.getKey())));
		
		return itemRarity;
	}
	
	private Map<Integer, Double> backpropUtilToComponents(Map<Integer, Double> finalOutputUtility, 
			Set<Integer> availableItemIds) {
		Map<Integer, Double> newFinalUtility = new HashMap<>(finalOutputUtility);

		Set<Integer> expandedItemIds = new HashSet<>();
		Set<Integer> fringe = finalOutputUtility.keySet();
		while (fringe.size() > 0) {
			Set<Integer> newFringe = new HashSet<>();
			for (int outputItemId: fringe) {
				double outputUtil = new Double((double) newFinalUtility.get(outputItemId));
				
				//System.out.println("-------------------------------------");
				//System.out.println("Starting item: " + ItemData.getNameFromId(outputItemId) + ", " + outputUtil);
				
				List<Process> processes = ProcessData.getProcessesByOutput(outputItemId);
				for (Process process: processes) {
					List<InventoryItem> inputs = process.inputItems;
					
					double totalItems = 0;
					for (InventoryItem item: inputs) {
						totalItems += item.quantity;
					}
					for (int itemIndex = 0; itemIndex < inputs.size(); itemIndex++) {
						InventoryItem item = inputs.get(itemIndex);
						double provisionalUtil = 0;
						if (newFinalUtility.containsKey(item.itemId)) {
							provisionalUtil = newFinalUtility.get(item.itemId);
						}
						double percentage = item.quantity / totalItems; // * provisionalUtil;
						
						//System.out.println("Sub-item: " + ItemData.getNameFromId(item.itemId));
						//System.out.println((int) (percentage * 100) + "%, " + (int) provisionalUtil + " util, " + (int) (percentage * (outputUtil + provisionalUtil) / 2.0) + " frac. util");
						//System.out.println("");
						
						MathUti.insertKeepMaxMap(newFinalUtility, item.itemId, percentage * (outputUtil + provisionalUtil) / 2.0);
						
						if (!expandedItemIds.contains(item.itemId)) {
							expandedItemIds.add(item.itemId);
							newFringe.add(item.itemId);
						}
					}
				}
				
				//System.out.println("");
			}
			fringe = newFringe;
		}
		return newFinalUtility;
	}
	
	/**
	 * @return A map of needs mapping to intensity values, based on this society's total needs for certain parts of the Maslow hierachy
	 */
	private Map<String, Double> findAllNeedsIntensity() {
		Map<String, Double> societalNeed = new HashMap<>();
		for (Human human : inhabitants) {
			double hungerScore = 1.0 - human.nutrition / human.maxNutrition;
			MathUti.addNumMap(societalNeed, "Eat", hungerScore);
		
			double thirstScore = 1.0 - human.hydration / human.maxHydration;
			MathUti.addNumMap(societalNeed, "Drink", thirstScore);
			
			int shelterScore = 0;
			for (Vector3i coords: human.allClaims) {
				LocalTile tile = grid.getTile(coords);
				if (tile != null) {
					LocalTile aboveTile = grid.getTile(coords);
					if (ItemData.isPlaceable(tile.tileBlockId) || 
							(aboveTile != null && ItemData.isPlaceable(aboveTile.tileFloorId)) ||
							(aboveTile != null && ItemData.isPlaceable(aboveTile.tileBlockId))) {
						shelterScore++;
					}
				}
			}
			
			int minShelter = 20;
			double normShelterScore = (double) Math.max(minShelter - shelterScore, 0) / minShelter;
			MathUti.addNumMap(societalNeed, "Shelter", normShelterScore);
			
			if (human.home == null) {
				MathUti.addNumMap(societalNeed, "Personal Home", 1.0);
				MathUti.addNumMap(societalNeed, "Furniture", 0.8);
			}
			else {
				double normFurnitureScore = 0;
				Set<Vector3i> buildingSpaces = human.home.calculatedLocations;
				for (Vector3i buildingSpace: buildingSpaces) {
					LocalTile tile = grid.getTile(buildingSpace);
					if (tile.building != null) {
						int id = tile.building.buildingId;
						List<ProcessStep> itemProps = ItemData.getItemProps(id);
						double beautyValue = ItemData.getItemBeautyValue(id);
						if (itemProps != null) {
							for (ProcessStep step: itemProps) {
								if (step.stepType.equals("Furniture")) {
									normFurnitureScore *= step.modifier * beautyValue;
								}
							}
						}
					}
				}
				normFurnitureScore = Math.min(normFurnitureScore, 1.0);
				MathUti.addNumMap(societalNeed, "Furniture", normFurnitureScore);
				MathUti.addNumMap(societalNeed, "Personal Home", normFurnitureScore / 3);
			}	
			
			double beautyScore = grid.averageBeauty(human.location.coords);
			MathUti.addNumMap(societalNeed, "Beauty", 1.0 - beautyScore);
		}
		return societalNeed;
	}
	
	/**
	 * @return Find a map of the direct utility generated by item
	 */
	private Map<String, Double> findUtilityByNeed(Integer itemId) {
		Map<String, Double> rawUtilByNeed = new HashMap<>();
		
		if (ItemData.isPlaceable(itemId)) {
			int pickupTime = ItemData.getPickupTime(itemId);
			MathUti.addNumMap(rawUtilByNeed, "Shelter", pickupTime / 100.0);
		}
		
		List<ProcessStep> itemActions = ItemData.getItemActions(itemId);
		if (itemActions != null) {
			for (ProcessStep step: itemActions) {
				MathUti.addNumMap(rawUtilByNeed, step.stepType, step.modifier);
			}
		}
		
		List<ProcessStep> itemProps = ItemData.getItemProps(itemId);
		if (itemProps != null) {
			for (ProcessStep step: itemProps) {
				MathUti.addNumMap(rawUtilByNeed, step.stepType, step.modifier);
			}
		}
		
		double baseValue = ItemData.getBaseItemValue(itemId);
		MathUti.addNumMap(rawUtilByNeed, "Wealth", baseValue);
		
		double beautyValue = ItemData.getItemBeautyValue(itemId);
		MathUti.addNumMap(rawUtilByNeed, "Beauty", beautyValue / 3.0);
		
		/*
		System.out.println(ItemData.getNameFromId(itemId) + " stats: ");
		System.out.println(rawUtilByNeed);
		System.out.println("---------------------");
		*/
		
		return rawUtilByNeed;
	}
	
	public Set<Integer> getBestBuildingMaterials(Map<Integer, Double> calcUtility, Human human, int desiredAmount) {
		Map<Integer, Double> rawResRarity = findRawResourcesRarity(human);
		Map<Integer, Double> bestBuildingMaterials = new HashMap<>();
		
		for (Entry<Integer, Double> entry: rawResRarity.entrySet()) {
			int itemId = entry.getKey();
			if (ItemData.isPlaceable(itemId) && ItemData.isValidBuildingMaterial(itemId)) {
				int pickupTime = ItemData.getPickupTime(itemId);
				double strength = Math.pow(pickupTime / 100.0, 1.5);
				bestBuildingMaterials.put(itemId, rawResRarity.get(itemId) * strength);
			}
		}
		
		//bestBuildingMaterials = MathUti.getSortedMapByValueDesc(bestBuildingMaterials);
		//int bestMaterialId = (int) bestBuildingMaterials.keySet().toArray()[0];
		//return bestMaterialId;
		return bestBuildingMaterials.keySet();
	}
	
}
