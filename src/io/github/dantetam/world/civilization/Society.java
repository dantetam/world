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
import io.github.dantetam.world.dataparse.Process;
import io.github.dantetam.world.dataparse.Process.ProcessStep;
import io.github.dantetam.world.dataparse.ProcessData;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.items.InventoryItem;

public class Society {

	public LocalGrid grid;
	private List<Human> inhabitants;
	
	public Society(LocalGrid grid) {
		this.grid = grid;
		inhabitants = new ArrayList<>();
	}
	
	public void addPerson(Human person) {
		inhabitants.add(person);
	}
	
	public Process findBestProcess(Map<Integer, Double> allItemsUtility, int outputItemId) {
		Set<Integer> visitedItemIds = new HashSet<>();
		Set<Integer> fringe = new HashSet<>();
		fringe.add(outputItemId);
		
		Map<Integer, Double> rawResRarity = findRawResourcesRarity();
		
		while (fringe.size() > 0) {
			Map<Process, Double> currentUtilCandidates = new HashMap<>();
			
			Set<Integer> newFringe = new HashSet<>();
			for (Integer fringeId: fringe) {
				
				System.out.println("Process for base item: " + ItemData.getNameFromId(fringeId));
				
				List<Process> processes = ProcessData.getProcessesByOutput(fringeId);
				for (Process process: processes) {
					List<InventoryItem> inputs = process.inputItems;
					if (inputs == null) inputs = new ArrayList<>();
					
					if (process.requiredBuildNameOrGroups != null) {
						for (String buildingName: process.requiredBuildNameOrGroups) {
							inputs.add(ItemData.item(buildingName, 1));
						}
					}
					
					System.out.println("Looking at process: " + process.toString());
					
					for (InventoryItem input: inputs) {
						double util = allItemsUtility.containsKey(input.itemId) ? 
								allItemsUtility.get(input.itemId) : 0;
						MathUti.insertKeepMaxMap(currentUtilCandidates, process, util);
						System.out.println("Found utility for base item: " + util);
						if (!visitedItemIds.contains(input.itemId)) {
							System.out.println("Expanding from " + ItemData.getNameFromId(fringeId)
									+ " -----> " + ItemData.getNameFromId(input.itemId));
							newFringe.add(input.itemId);
						}
					}
					
				}
				visitedItemIds.add(fringeId);
			}
			
			currentUtilCandidates = MathUti.getSortedMapByValue(currentUtilCandidates);
			
			Object[] orderedProcesses = currentUtilCandidates.keySet().toArray();
			for (Object obj: orderedProcesses) {
				Process process = (Process) obj;
				System.out.println(canCompleteProcess(process, rawResRarity) + "<<<<32");
				if (canCompleteProcess(process, rawResRarity)) {
					return process;
				}
			}
			
			fringe = newFringe;
		}
		
		return null;
	}
	
	//Return the necessary items that are needed for a process
	public boolean canCompleteProcess(Process process, Map<Integer, Double> rawResRarity) {
		//Check for resources
		List<InventoryItem> inputs = process.inputItems;
		for (InventoryItem input: inputs) {
			if (!rawResRarity.containsKey(input.itemId) || rawResRarity.get(input.itemId) == 0) {
				return false;
			}
		}
		
		//Check for buildings
		if (process.requiredBuildNameOrGroups != null) {
			for (String buildingName: process.requiredBuildNameOrGroups) {
				int buildingId = ItemData.getIdFromName(buildingName);
				if (!rawResRarity.containsKey(buildingId) || rawResRarity.get(buildingId) == 0) {
					return false;
				}
			}
		}
			
		//TODO: Check for required location and/or site
		return true;
	}
	
	/**
	 * @return A mapping of every item in the world available to its calcuated utility.
	 */
	public Map<Integer, Double> findCompleteUtilityAllItems() {
		Map<Integer, Double> allRarity = findAllAvailableResourceRarity();
		Set<Integer> availableItemIds = allRarity.keySet();
		
		Map<String, Double> needsIntensity = findAllNeedsIntensity();
		
		//Used to normalize the values and determine 
		Map<String, Double> totalNeedsUtility = new HashMap<>(); 
		
		Map<Integer, Double> finalOutputUtility = new HashMap<>();
		
		for (int itemId : availableItemIds) {
			double itemRarity = new Double(Math.log10(allRarity.get(itemId)));
			
			Function<Entry<String, Double>, Double> utilCalcBalance = e -> {
				Double intensity = needsIntensity.get(e.getKey());
				if (intensity == null) {
					intensity = new Double(0.5);
				}
				return e.getValue() * intensity / itemRarity;
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
		
		Map<String, Double> sortedNeedUtility = MathUti.getSortedMapByValue(totalNeedsUtility);
		
		Map<String, Double> needWeights = new HashMap<>();
		
		//for 
		
		return propogatedFinalUtil;
	}
	
	/**
	 * Directly count the physical rarity and presence of all items available on the map
	 */
	private Map<Integer, Double> findRawResourcesRarity() {
		Map<Integer, Double> itemRarity = new HashMap<>();
		for (int r = 0; r < grid.rows; r++) {
			for (int c = 0; c < grid.cols; c++) {
				int startHeight = grid.findLowestEmptyHeight(r, c) - 1;
				for (int h = startHeight; h >= 0; h--) {
					LocalTile tile = grid.getTile(new Vector3i(r, c, h));
					if (tile == null) continue;
					if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID) {
						ItemTotalDrops drops = ItemData.getOnBlockItemDrops(tile.tileBlockId);
						Map<Integer, Double> itemExpectations = drops.itemExpectation();
						for (Entry<Integer, Double> entry: itemExpectations.entrySet()) {
							MathUti.addNumMap(itemRarity, entry.getKey(), entry.getValue());
						}
					}
					if (tile.itemOnFloor != null) {
						int id = tile.itemOnFloor.itemId;
						int num = tile.itemOnFloor.quantity;
						MathUti.addNumMap(itemRarity, id, (double) num);
					}
				}
			}
		}
		for (Human human: inhabitants) {
			List<InventoryItem> items = human.inventory.getItems();
			for (InventoryItem item: items) {
				MathUti.addNumMap(itemRarity, item.itemId, (double) item.quantity);
			}
		}
		return itemRarity;
	}
	
	/**
	 * Using raw resources, figure out every possible item that could be generated from destroying,
	 * crafting, or otherwise processing the available items. Return all possible items that this
	 * society could create, and their associated rareness.
	 */
	private Map<Integer, Double> findAllAvailableResourceRarity() {
		Set<Integer> visitedItemIds = new HashSet<>();
		Set<Integer> fringe = new HashSet<>();
		Map<Integer, Double> rawResources = findRawResourcesRarity();
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
				
				System.out.println("Starting item: " + ItemData.getNameFromId(outputItemId) + ", "
						+ outputUtil);
				
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
						if (finalOutputUtility.containsKey(item.itemId)) {
							provisionalUtil = finalOutputUtility.get(item.itemId);
						}
						double percentage = item.quantity / totalItems * provisionalUtil;
						
						System.out.println("Sub-item: " + ItemData.getNameFromId(item.itemId));
						System.out.println(percentage + " " + provisionalUtil + " " + percentage * outputUtil);
						
						MathUti.insertKeepMaxMap(newFinalUtility, item.itemId, percentage * outputUtil);
						
						if (!expandedItemIds.contains(item.itemId)) {
							expandedItemIds.add(item.itemId);
							newFringe.add(item.itemId);
						}
					}
				}
				
				System.out.println("");
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
				if (step.stepType.equals("Eat")) {
					MathUti.addNumMap(rawUtilByNeed, "Eat", step.modifier);
				}
				else if (step.stepType.equals("Rest")) {
					MathUti.addNumMap(rawUtilByNeed, "Rest", step.modifier);
				}
			}
		}
		
		double baseValue = ItemData.getBaseItemValue(itemId);
		MathUti.addNumMap(rawUtilByNeed, "Wealth", baseValue);
		
		System.out.println(ItemData.getNameFromId(itemId) + " stats: ");
		System.out.println(rawUtilByNeed);
		System.out.println("---------------------");
		
		return rawUtilByNeed;
	}
	
}
