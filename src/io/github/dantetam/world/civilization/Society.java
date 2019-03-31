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
	
	private Map<Integer, Double> findRawResourcesRarity() {
		Map<Integer, Double> itemRarity = new HashMap<>();
		for (int r = 0; r < grid.rows; r++) {
			for (int c = 0; c < grid.cols; c++) {
				int startHeight = grid.findLowestEmptyHeight(r, c) - 1;
				for (int h = startHeight; h >= 0; h--) {
					LocalTile tile = grid.getTile(new Vector3i(r, c, h));
					if (tile.tileBlockId != ItemData.ITEM_EMPTY_ID) {
						ItemTotalDrops drops = ItemData.getOnBlockItemDrops(tile.tileBlockId);
						Map<Integer, Double> itemExpectations = drops.itemExpectation();
						for (Entry<Integer, Double> entry: itemExpectations.entrySet()) {
							double prevEntry = itemRarity.containsKey(entry.getKey()) ? itemRarity.get(entry.getKey()) : 0;
							itemRarity.put(entry.getKey(), prevEntry + entry.getValue());
						}
					}
					if (tile.itemOnFloor != null) {
						int id = tile.itemOnFloor.itemId;
						int num = tile.itemOnFloor.quantity;
						double prevEntry = itemRarity.containsKey(id) ? itemRarity.get(id) : 0;
						itemRarity.put(id, prevEntry + num);
					}
				}
			}
		}
		return itemRarity;
	}
	
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
				List<Integer> processIds = ProcessData.recipesByInput.get(itemId);
				if (processIds != null) {
					for (Integer processId: processIds) {
						Process process = ProcessData.processes.get(processId);
						Map<Integer, Double> itemExpectations = process.outputItems.itemExpectation();
						for (Entry<Integer, Double> entry: itemExpectations.entrySet()) {
							int nextItemId = entry.getKey();
							//Expand the item's process chain if necessary, otherwise just add to a running count
							if (!visitedItemIds.contains(nextItemId)) {
								newFringe.add(nextItemId);
							}
							double prevEntry = itemRarity.containsKey(nextItemId) ? itemRarity.get(nextItemId) : 0;
							itemRarity.put(nextItemId, prevEntry + entry.getValue());
						}
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
	
	public Map<Integer, Double> findCompleteUtilityAllItems() {
		Map<Integer, Double> allRarity = findAllAvailableResourceRarity();
		Set<Integer> availableItemIds = allRarity.keySet();
		
		Map<String, Double> needsIntensity = findAllNeedsIntensity();
		
		//Used to normalize the values and determine 
		Map<String, Double> totalNeedsUtility = new HashMap<>(); 
		
		Map<Integer, Double> finalUtility = new HashMap<>();
		
		for (int itemId : availableItemIds) {
			double itemRarity = allRarity.get(itemId);
			
			Function<Entry<String, Double>, Double> utilCalcBalance = e -> {
				Double intensity = needsIntensity.get(e.getKey());
				if (intensity == null) {
					intensity = new Double(1.0);
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
			
			finalUtility.put(itemId, sumWeightsUtility);
			
			/*
			Map<String, Double> needsUtilityFromItems = findUtilityByNeed(itemId);
			needsUtilityFromItems.entrySet().stream().collect(Collectors.toMap(Entry::getKey,
					e -> e.getValue() * needsIntensity.get(e.getKey()) / itemRarity
					));
			*/
		}
		
		Map<String, Double> sortedNeedUtility = MathUti.sortMapByValue(totalNeedsUtility);
		
		Map<String, Double> needWeights = new HashMap<>();
		
		//for 
		
		return finalUtility;
	}
	
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
			MathUti.addNumMap(societalNeed, "Shelter", (double) Math.max(minShelter - shelterScore, 0));
		}
		return societalNeed;
	}
	
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
		
		return rawUtilByNeed;
	}
	
}
