package io.github.dantetam.world.civilization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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

import io.github.dantetam.toolbox.CollectionUtil;
import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.Pair;
import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civhumanai.Ethos;
import io.github.dantetam.world.civhumanai.NeedsGamut;
import io.github.dantetam.world.civhumanrelation.EmotionGamut;
import io.github.dantetam.world.civhumansocietyai.SocietalHumansActionsCalc;
import io.github.dantetam.world.combat.War;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ItemTotalDrops;
import io.github.dantetam.world.dataparse.ProcessData;
import io.github.dantetam.world.grid.LocalBuilding;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalGridLandClaim;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.grid.execute.LocalGridTimeExecution;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.process.LocalJob;
import io.github.dantetam.world.process.LocalProcess;
import io.github.dantetam.world.process.LocalProcess.ProcessStep;
import io.github.dantetam.world.process.LocalSocietyJob;

public class Society {

	public String name;
	public LocalGrid primaryGrid;
	public List<LocalGrid> allGrids;
	private List<Household> households;
	public Vector3i societyCenter;
	
	public SocietyLeadership leadershipManager;
	
	public List<War> warsInvolved;
	
	public String dominantCultureStr;
	
	public JobMarket jobMarket;
	public Map<LocalProcess, LocalSocietyJob> societalJobQueue;
	
	//Societal utility of objects measured for this society, stored here
	public Map<Integer, Double> calcUtility; //Sorted ascending
	public Map<Integer, Double> accessibleResUtil; 
	public Map<String, Double> groupItemRarity;
	public Map<Integer, Double> adjEconomicUtility;
	public List<Vector3i> importantLocations;
	public Map<Human, NeedsGamut> humanNeedsMap; //Fill with the needs of humans calculated in needs intensity calc.
	public NeedsGamut totalNeedsGamut;
	
	//This society's unique claims on areas of land
	//This can conflict and overlap with other societies' deeds and claims to all global land
	private Map<LocalGrid, Human[][][]> landClaimRefByTile;
	
	public Society(String name, LocalGrid grid) {
		this.name = name;
		
		landClaimRefByTile = new HashMap<>();
		
		this.allGrids = new ArrayList<>();
		this.primaryGrid = grid;
		addGrid(this.primaryGrid);
		
		households = new ArrayList<>();
		warsInvolved = new ArrayList<>();
		this.leadershipManager = new SocietyLeadership(this);
		this.jobMarket = new JobMarket();
		this.societalJobQueue = new HashMap<>();
	}
	
	public void addHousehold(Household house) {
		households.add(house);
		house.society = this;
	}
	
	public void removeHousehold(Household house) {
		households.remove(house);
		house.society = null;
	}
	
	public void addGrid(LocalGrid grid) {
		this.allGrids.add(grid);
		Human[][][] arr = new Human[grid.rows][grid.cols][grid.heights];
		landClaimRefByTile.put(grid, arr);
	}
	
	public void recordClaimInGrid(Human human, LocalGrid grid, Vector3i vec) {
		if (!human.society.equals(this)) {
			throw new IllegalArgumentException("Cannot record land claim for human of society: " + human.society.toString() +
					" in this society: " + this.name);
		}
		if (!landClaimRefByTile.containsKey(grid)) {
			addGrid(grid);
		}
		if (grid.inBounds(vec)) {
			landClaimRefByTile.get(grid)[vec.x][vec.y][vec.z] = human;
		}
	}
	
	public Human getRecordClaimInGrid(LocalGrid grid, Vector3i vec) {
		if (!landClaimRefByTile.containsKey(grid)) {
			throw new IllegalArgumentException("Cannot find records for grid: " + grid.toString());
		}
		if (!grid.inBounds(vec)) {
			throw new IllegalArgumentException("In grid: " + grid + ", vec out of bounds: " + vec);
		}
		return landClaimRefByTile.get(grid)[vec.x][vec.y][vec.z];
	}
	
	public List<Human> getAllPeople() {
		List<Human> people = new ArrayList<>();
		for (Household house: this.households) {
			for (Human human: house.householdMembers) {
				people.add(human);
			}
		}
		return people;
	}
	
	public List<Household> getAllHouseholds() {
		return households;
	}
	
	public double getTotalWealth() {
		double wealth = 0;
		for (Household household: this.households) {
			wealth += household.getTotalWealth();
		}
		return wealth;
	}
	
	/**
	 * Find important locations to travel between within a society, for patrol purposes.
	 * These locations include homes, businesses, resource stores, and so on.
	 * 
	 * @param centerCoords Find locations closest to these given arbitrary coords
	 */
	public List<Vector3i> getImportantLocations(Vector3i centerCoords) {
		Collection<Vector3i> nearBuildings = primaryGrid.getNearestBuildings(centerCoords);
		Collection<Vector3i> humanLocations = primaryGrid.getNearestPeopleCoords(centerCoords);
		List<Vector3i> results = new ArrayList<>();
		for (Vector3i nearBuilding: nearBuildings) {
			results.add(nearBuilding);
		}
		for (Vector3i humanLocation: humanLocations) {
			results.add(humanLocation);
		}
		return results;
	}
	
	public void createJobOffers() {
		this.jobMarket.createJobOffers(this, primaryGrid);
	}
	
	/**
	 * 
	 * @param allItemsUtility A mapping of all items linked to their societal utility
	 * @param human The human in question who has access to resources, buildings, etc.
	 * @param desiredNumProcess The number of processes returned in this search
	 * @param desiredItems The restriction to only search for processes 
	 * @return
	 */
	public Map<LocalProcess, Double> prioritizeProcesses(Map<Integer, Double> allItemsUtility, 
			LocalGrid grid, Human human, int desiredNumProcess, Set<Integer> desiredItems) {
		Map<Integer, Double> sortedUtility = MapUtil.getSortedMapByValueDesc(allItemsUtility);
		Object[] sortedKeys = sortedUtility.keySet().toArray();
		
		Map<Integer, Double> rawResRarity = findRawResourcesRarity(human);
		
		Map<LocalProcess, Double> processByUtil = new HashMap<>();
		int i = 0;
		while (i < sortedKeys.length) { //processByUtil.size() < desiredNumProcess
			Integer itemId = (Integer) sortedKeys[i];
			Map<LocalProcess, Double> bestProcesses = findBestProcess(allItemsUtility, rawResRarity, 
					grid, human, itemId);
			
			if (bestProcesses != null) {
				if (desiredItems == null || desiredItems.contains(itemId)) {
					//processByUtil.put(bestProcess, sortedUtility.get(itemId));
					for (Entry<LocalProcess, Double> entry: bestProcesses.entrySet()) {
						MapUtil.insertKeepMaxMap(processByUtil, entry.getKey(), entry.getValue());
					}
				}
			}
			i++;
		}
		
		//For leftover processes that have not been assigned a utility yet
		NeedsGamut needsIntensity = finalTotalNeedsMap();
		for (LocalProcess process: ProcessData.getAllProcesses()) {
			if (!processByUtil.containsKey(process)) {
				double heuristicActionScore = 0;
				List<ProcessStep> resActions = process.processResActions;
				if (resActions != null) {
					for (ProcessStep resAction: resActions) {
						double needWeight = 0.5;
						if (needsIntensity.hasEmotion(resAction.stepType)) {
							needWeight = needsIntensity.getEmotion(resAction.stepType);
						}
						heuristicActionScore += needWeight * resAction.modifier;
					}
				}
				if (heuristicActionScore > 0)
					processByUtil.put(process.clone(), heuristicActionScore);
			}
		}
		
		Map<LocalProcess, Double> processByPercentage = MapUtil.getNormalizedMap(processByUtil);
		processByPercentage = MapUtil.getSortedMapByValueDesc(processByPercentage);
		
		/*
		CustomLog.outPrintln("Ranked processes: #####");
		for (Entry<LocalProcess, Double> entry: processByUtil.entrySet()) {
			CustomLog.outPrintln(entry.getKey().name + "; ranking: " + entry.getValue());
		}
		*/
		
		return processByPercentage;
	}
	
	public Map<LocalJob, Double> prioritizeJobs(Human human, 
			Map<LocalProcess, Double> processUtil, Date date) {
		Map<LocalJob, Double> rankedJobs = new HashMap<>();
		
		for (Entry<LocalProcess, LocalJob> entry: this.jobMarket.allJobsAvailable.entrySet()) {
			double physicalUtil = processUtil.containsKey(entry.getKey()) ? processUtil.get(entry.getKey()) : 0;
			double workProsUtil = SocietalHumansActionsCalc
					.possibleEmployeeUtil(human, entry.getValue().boss, entry.getValue(), date);
			rankedJobs.put(entry.getValue(), (physicalUtil + workProsUtil) / 2);
		}
		
		for (Entry<LocalProcess, LocalSocietyJob> entry: this.societalJobQueue.entrySet()) {
			double physicalUtil = processUtil.containsKey(entry.getKey()) ? processUtil.get(entry.getKey()) : 0;
			double workProsUtil = SocietalHumansActionsCalc
					.employeeSocietyUtil(human, entry.getValue().societyJob, entry.getValue(), date);
			rankedJobs.put(entry.getValue(), (physicalUtil + workProsUtil) / 2);
		}
		
		rankedJobs = MapUtil.getNormalizedMap(rankedJobs);
		rankedJobs = MapUtil.getSortedMapByValueDesc(rankedJobs);
		return rankedJobs;
	}
	
	/**
	 * Return the first process at the highest level (closest to final product),
	 * which is possible to complete by this human, by just crafting and collecting raw resources.
	 * @param human The human in question who has access to resources, buildings, etc.
	 */
	public Map<LocalProcess, Double> findBestProcess(Map<Integer, Double> allItemsUtility, 
			Map<Integer, Double> rawResRarity, LocalGrid grid, Human human, int outputItemId) {
		Set<Integer> visitedItemIds = new HashSet<>();
		Set<Integer> fringe = new HashSet<>();
		fringe.add(outputItemId);
		
		Map<LocalProcess, Double> bestProcesses = new HashMap<>();
		
		//CustomLog.outPrintln("raw resource rarity: ");
		//CustomLog.outPrintln(rawResRarity);
		
		//CustomLog.outPrintln("_______________________________________" + ItemData.getNameFromId(outputItemId));
		
		while (fringe.size() > 0) {
			Set<Integer> newFringe = new HashSet<>();
			for (Integer fringeId: fringe) {
				
				//CustomLog.outPrintln("Process for base item >>>: " + ItemData.getNameFromId(fringeId));
				
				List<LocalProcess> processes = ProcessData.getProcessesByOutput(fringeId);
				for (LocalProcess process: processes) {
					//TODO: Factor in counts into util. backprop calc.
					if (canCompleteProcess(process, rawResRarity, grid, human)) {
						List<InventoryItem> inputs = new ArrayList<>();
						if (process.requiredBuildNameOrGroup != null) {
							for (Integer id: ItemData.getIdsFromNameOrGroup(process.requiredBuildNameOrGroup)) {
								inputs.add(ItemData.createItem(id, 1));
							}
						}
						if (process.requiredTileNameOrGroup != null) {
							for (Integer id: ItemData.getIdsFromNameOrGroup(process.requiredTileNameOrGroup)) {
								inputs.add(ItemData.createItem(id, 1));
							}
						}
						for (InventoryItem input: process.inputItems) {
							inputs.add(input);
						}
						
						for (InventoryItem input: inputs) {
							double util = allItemsUtility.containsKey(input.itemId) ? 
									allItemsUtility.get(input.itemId) : 0;
							
							MapUtil.insertKeepMaxMap(bestProcesses, process.clone(), util);
	
							if (!visitedItemIds.contains(input.itemId)) {
								if (util > 0)
									newFringe.add(input.itemId);
									//CustomLog.outPrintln("Expanding from " + ItemData.getNameFromId(fringeId) + " -----> " + ItemData.getNameFromId(input.itemId));
							}
						}
					}
				}
				visitedItemIds.add(fringeId);
			}	
			fringe = newFringe;
		}
		
		bestProcesses = MapUtil.getSortedMapByValueAsc(bestProcesses);
		return bestProcesses;
	}
	
	//Return the necessary items that are needed for a process
	public boolean canCompleteProcess(LocalProcess process, Map<Integer, Double> rawResRarity,
			LocalGrid grid, LivingEntity being) {
		//Check for resources
		List<InventoryItem> inputs = process.inputItems;
		for (InventoryItem input: inputs) {
			if (!rawResRarity.containsKey(input.itemId) || rawResRarity.get(input.itemId) == 0) {
				//CustomLog.outPrintln("No input");
				return false;
			}
			
			Map<Integer, Integer> itemsAmtNeeded = new HashMap<>();
			itemsAmtNeeded.put(input.itemId, input.quantity);
			if (!LocalGridTimeExecution.hasAccessToItem(grid, being, itemsAmtNeeded)) {
				return false;
			}
		}
		
		//Check for buildings
		if (process.requiredBuildNameOrGroup != null) {
			Set<Integer> buildingIds = ItemData.getIdsFromNameOrGroup(process.requiredBuildNameOrGroup);
			Collection<Integer> intersectIds = CollectionUtil.colnsIntersection(rawResRarity.keySet(), 
					buildingIds);
			if (intersectIds.size() == 0) {
				//CustomLog.outPrintln("No req building");
				return false;
			}
			else {
				boolean foundItem = false;
				for (Integer id: intersectIds) {
					if (rawResRarity.get(id) > 0) {
						foundItem = true;
					}
				}
				if (!foundItem) {
					return false;
				}
			}
		}
		
		if (process.requiredTileNameOrGroup != null) {
			Set<Integer> buildingIds = ItemData.getIdsFromNameOrGroup(process.requiredTileNameOrGroup);
			Collection<Integer> intersectIds = CollectionUtil.colnsIntersection(rawResRarity.keySet(), 
					buildingIds);
			if (intersectIds.size() == 0) {
				//CustomLog.outPrintln("No req tile");
				return false;
			}
			else {
				for (Integer id: intersectIds) {
					if (rawResRarity.get(id) > 0) {
						//The preliminary item finding test is successful
						//Use the full access/tile search algorithm to determine truly if the process has a tile
						//return true;
						//TODO
						
						Pair<LocalTile> potentialTiles = LocalGridTimeExecution.assignTile(
								grid, being, new HashSet<Human>() {{
									if (being instanceof Human)
										add((Human) being);
								}}, process);
						return potentialTiles != null;
						
					}
				}
				return false;
			}
		}
			
		//TODO //Check for required location and/or site
		return true;
	}
	
	//Scrap the idea of storage since there is no good SWE way to track time of utility calculation
	public Map<Integer, Double> getCalcUtil() {
		if (this.calcUtility == null) {
			this.calcUtility = this.findCompleteUtilityAllItems(null);
		}
		return this.calcUtility;
	}
	
	/**
	 * @return A mapping of every item in the world available to its calcuated utility.
	 */
	public Map<Integer, Double> findCompleteUtilityAllItems(Human human) {
		Map<Integer, Double> allRarity = findAllAvailableResourceRarity(human);
		Map<Integer, Double> economicRarityMap = this.adjEconomicUtility == null ? 
				findAdjEconomicRarity() : this.adjEconomicUtility;
		Set<Integer> availableItemIds = economicRarityMap.keySet();
		
		NeedsGamut needsIntensity = finalTotalNeedsMap();
		
		Map<String, Double> needWeights = new HashMap<>();
		needWeights.put("Eat", 2.0);
		needWeights.put("Rest", 2.5);
		needWeights.put("Shelter", 1.0);
		needWeights.put("Soldier", 0.3);
		
		Map<String, Double> productWeights = needsIntensity.productWeights(needWeights);
		
		//Used to normalize the values and determine 
		Map<String, Double> totalNeedsUtility = new HashMap<>(); 
		Map<Integer, Double> finalOutputUtility = new HashMap<>();
		
		for (int itemId : availableItemIds) {
			double itemRarity = allRarity.containsKey(itemId) ? 
					Math.max(0, Math.log10(allRarity.get(itemId))) : 0;
			double economicRarity = economicRarityMap.containsKey(itemId) ? 
					Math.max(0, Math.log10(economicRarityMap.get(itemId))) : 0;
			
			itemRarity = Math.min(itemRarity, 10);
			economicRarity = Math.min(economicRarity, 10);
					
			//Develop a basic utility value: item use * need for item use / rareness
			Map<String, Double> needsUtilityFromItems = findUtilityByNeed(itemId);
			
			for (Entry<String, Double> e: needsUtilityFromItems.entrySet()) {
				Double intensity = productWeights.get(e.getKey());
				if (intensity == null) {
					intensity = 0.5;
				}
				//double needWeight = needWeights.containsKey(e.getKey()) ? needWeights.get(e.getKey()) : 0.5;
				double util = (e.getValue() * intensity) / (itemRarity * economicRarity + 1);
				needsUtilityFromItems.put(e.getKey(), util);
			}
			
			double sumWeightsUtility = 0;
			for (Entry<String, Double> entry: needsUtilityFromItems.entrySet()) {
				MapUtil.addNumMap(totalNeedsUtility, entry.getKey(), entry.getValue());
				sumWeightsUtility += entry.getValue();
			}
			
			double itemEthosAdj = 1;
			if (human != null) {
				Ethos itemEthos = human.brain.ethosSet.ethosTowardsItems.get(itemId);
				itemEthosAdj = itemEthos.getLogisticVal(0, 3.5);
			}
			sumWeightsUtility *= itemEthosAdj;
			
			finalOutputUtility.put(itemId, sumWeightsUtility);
		}
		
		Map<Integer, Double> propogatedFinalUtil = backpropUtilToComponents(
				finalOutputUtility, availableItemIds, human);
		propogatedFinalUtil = MapUtil.getSortedMapByValueAsc(propogatedFinalUtil);
		return propogatedFinalUtil;
	}
	
	/**
	 * Directly count the physical rarity and presence of all items available on the map
	 * 
	 * @param The owner of the inventory/property that this alg. should count items.
	 * 		  If null, return a generic count of all items in the world, 
	 *        not factoring for ownership/restriction.
	 */
	public Map<Integer, Double> findRawResourcesRarity(Human human) {
		Map<Integer, Double> preItemRarity = new HashMap<Integer, Double>();
		for (LocalGrid grid: this.allGrids) {
			MapUtil.addMapToMap(preItemRarity, grid.tileIdCounts);
		}
		Map<Integer, Double> itemRarity = new HashMap<>(preItemRarity);
		
		for (Entry<Integer, Double> entry: preItemRarity.entrySet()) {
			Integer itemId = entry.getKey();
			Double count = entry.getValue();
			
			ItemTotalDrops drops = ItemData.getOnBlockItemDrops(itemId);
			Map<Integer, Double> itemExpectations = drops.itemExpectation();
			for (Entry<Integer, Double> dropEntry: itemExpectations.entrySet()) {
				MapUtil.addNumMap(itemRarity, dropEntry.getKey(), dropEntry.getValue() * count);
			}
		}
		
		if (human == null) {
			for (LocalGrid grid: this.allGrids) {
				for (LocalBuilding building: grid.getAllBuildings()) {
					if (building.owner == null) {
						MapUtil.addNumMap(itemRarity, building.buildingId, 1.0);
						for (int itemId: building.buildingBlockIds) {
							ItemTotalDrops drops = ItemData.getOnBlockItemDrops(itemId);
							Map<Integer, Double> itemExpectations = drops.itemExpectation();
							for (Entry<Integer, Double> entry: itemExpectations.entrySet()) {
								MapUtil.addNumMap(itemRarity, entry.getKey(), entry.getValue());
							}
						}
					}
				}
			}
			
			for (Human everyHuman: this.getAllPeople()) {
				List<InventoryItem> items = everyHuman.inventory.getItems();
				for (InventoryItem item: items) {
					MapUtil.addNumMap(itemRarity, item.itemId, (double) item.quantity);
				}
			}
		}
		else {
			for (LocalBuilding building: human.ownedBuildings) {
				MapUtil.addNumMap(itemRarity, building.buildingId, 1.0);
				for (int itemId: building.buildingBlockIds) {
					ItemTotalDrops drops = ItemData.getOnBlockItemDrops(itemId);
					Map<Integer, Double> itemExpectations = drops.itemExpectation();
					for (Entry<Integer, Double> entry: itemExpectations.entrySet()) {
						MapUtil.addNumMap(itemRarity, entry.getKey(), entry.getValue());
					}
				}
			}
			
			List<InventoryItem> items = human.inventory.getItems();
			for (InventoryItem item: items) {
				MapUtil.addNumMap(itemRarity, item.itemId, (double) item.quantity);
			}
		}
		return itemRarity;
	}
	
	/**
	 * Directly measure the rarity of all item groups, defined as item count in a group summed up
	 */
	public Map<String, Double> findRawGroupsResRarity(Human human) {
		Map<String, Double> groupItemRarity = new HashMap<>();
		Map<Integer, Double> itemRarity = this.findRawResourcesRarity(human);
		for (Entry<Integer, Double> entry: itemRarity.entrySet()) {
			Set<String> groupNames = ItemData.getGroupNameById(entry.getKey());
			if (groupNames != null) {
				for (String groupName: groupNames) {
					MapUtil.addNumMap(groupItemRarity, groupName, entry.getValue());
				}
			}
		}
		return groupItemRarity;
	}
	
	/**
	 * Directly count the adjusted economic rarity of all items on the map i.e.
	 * the inherent value of items on a 'free market'. This is useful for not having
	 * too many people produce the same resource, i.e. if wooden walls are the most
	 * profitable good when there are none, but 
	 */
	public Map<Integer, Double> findAdjEconomicRarity() {
		Map<Integer, Double> itemRarity = this.accessibleResUtil == null ? 
				this.findAllAvailableResourceRarity(null) : this.accessibleResUtil;
		
		//Make this method more efficient
		for (LocalGrid grid: this.allGrids) {
			for (LocalBuilding building: grid.getAllBuildings()) {
				for (int itemId: building.buildingBlockIds) {
					ItemTotalDrops drops = ItemData.getOnBlockItemDrops(itemId);
					Map<Integer, Double> itemExpectations = drops.itemExpectation();
					for (Entry<Integer, Double> entry: itemExpectations.entrySet()) {
						MapUtil.addNumMap(itemRarity, entry.getKey(), entry.getValue());
					}
				}
			}
		}
		for (Human everyHuman: this.getAllPeople()) {
			List<InventoryItem> items = everyHuman.inventory.getItems();
			for (InventoryItem item: items) {
				MapUtil.addNumMap(itemRarity, item.itemId, (double) item.quantity);
			}
			if (everyHuman.processProgress != null) {
				ItemTotalDrops drops = everyHuman.processProgress.outputItems;
				if (drops != null) {
					Map<Integer, Double> itemExpectations = drops.itemExpectation();
					for (Entry<Integer, Double> entry: itemExpectations.entrySet()) {
						MapUtil.addNumMap(itemRarity, entry.getKey(), entry.getValue() * 5);
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
	 * 
	 * Higher numbers means the resource is more common and has a higher count
	 */
	public Map<Integer, Double> findAllAvailableResourceRarity(Human human) {
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
				List<LocalProcess> inputProcessses = ProcessData.getProcessesByInput(itemId);
				
				//Collect all possible items that can come from breaking or crafting with itemId
				List<ItemTotalDrops> allItemDrops = new ArrayList<>();
				ItemTotalDrops onBlockDropExp = ItemData.getOnBlockItemDrops(itemId);
				if (onBlockDropExp != null && ItemData.isPlaceable(itemId)) {
					allItemDrops.add(onBlockDropExp);
				}
				for (LocalProcess process: inputProcessses) {
					if (process.outputItems != null) {
						allItemDrops.add(process.outputItems);
					}
				}
				
				for (ItemTotalDrops itemDrop: allItemDrops) {
					Map<Integer, Double> itemExpectations = itemDrop.itemExpectation();
					for (Entry<Integer, Double> entry: itemExpectations.entrySet()) {
						int nextItemId = entry.getKey();
						//Expand the item's process chain if necessary, otherwise just add to a running count
						if (!visitedItemIds.contains(nextItemId)) {
							newFringe.add(nextItemId);
						}
						//CustomLog.outPrintln(ItemData.getNameFromId(itemId) + " ---expand---> " + ItemData.getNameFromId(nextItemId));
						MapUtil.addNumMap(itemRarity, nextItemId, entry.getValue());
					}
				}
				
				visitedItemIds.add(itemId);
			}
			fringe = newFringe;
		}
		
		//itemRarity.entrySet().stream().collect(Collectors.toMap(Entry::getKey,
				//e -> e.getValue() * ItemData.getBaseItemValue(e.getKey())));
		
		return itemRarity;
	}
	
	private Map<Integer, Double> backpropUtilToComponents(Map<Integer, Double> finalOutputUtility, 
			Set<Integer> availableItemIds, Human human) {
		
		System.err.println("####################:");
		System.err.println(MapUtil.getSortedMapByValueAsc(finalOutputUtility));
		
		Map<Integer, Double> newFinalUtility = new HashMap<>(finalOutputUtility);

		Set<Integer> expandedItemIds = new HashSet<>();
		Set<Integer> fringe = finalOutputUtility.keySet();
		while (fringe.size() > 0) {
			Set<Integer> newFringe = new HashSet<>();
			for (int outputItemId: fringe) {
				double outputUtil = newFinalUtility.get(outputItemId);
				
				//CustomLog.outPrintln("-------------------------------------");
				//CustomLog.outPrintln("Starting item: " + ItemData.getNameFromId(outputItemId) + ", " + outputUtil);
				
				List<LocalProcess> processes = ProcessData.getProcessesByOutput(outputItemId);
				for (LocalProcess process: processes) {
					List<InventoryItem> inputs = process.inputItems;
					
					double inputCosts = 0;
					for (Integer itemId: process.heurCapitalInputs()) {
						if (newFinalUtility.containsKey(itemId)) {
							double inputUtil = newFinalUtility.get(itemId);
							inputCosts += inputUtil;
						}
					}
					
					double processWeight = 1;
					if (human != null) {
						Ethos processEthos = human.brain.ethosSet.ethosTowardsProcesses.get(process);
						processWeight = processEthos.getLogisticVal(0, 3.5);
					}
					
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
						double percentage = item.quantity / totalItems;
						
						//CustomLog.outPrintln("Sub-item: " + ItemData.getNameFromId(item.itemId));
						//CustomLog.outPrintln((int) (percentage * 100) + "%, " + (int) provisionalUtil + " util, " + (int) (percentage * (outputUtil + provisionalUtil) / 2.0) + " frac. util");
						//CustomLog.outPrintln("");
						
						double avgUtil = (outputUtil + provisionalUtil) / 2.0;
						avgUtil -= inputCosts;
						if (process.totalSupervisedTime() > 0)
							avgUtil /= process.totalSupervisedTime();
						
						MapUtil.insertKeepMaxMap(newFinalUtility, item.itemId, percentage * avgUtil * processWeight);
						
						if (!expandedItemIds.contains(item.itemId)) {
							expandedItemIds.add(item.itemId);
							newFringe.add(item.itemId);
						}
					}
				}
				
				CustomLog.outPrintln("");
			}
			fringe = newFringe;
		}
		return newFinalUtility;
	}
	
	/**
	 * @return A map of needs mapping to intensity values, based on this society's total needs for certain parts of the Maslow hierachy
	 * Return a Maslow's need hierarchy object, see NeedsGamut.java/StringDoubleGamut.java
	 */
	public NeedsGamut finalTotalNeedsMap() {
		NeedsGamut societalNeed = new NeedsGamut();
		for (Human human : this.getAllPeople()) {
			NeedsGamut humanNeed = findAllNeedsIntensHuman(human);
			societalNeed.addKeyGamut(humanNeed);
		}
		return societalNeed;
	}
	public Map<Human, NeedsGamut> findAllNeedsIntensityMap() {		
		Map<Human, NeedsGamut> humanMapNeeds = new HashMap<>();
		for (Human human : this.getAllPeople()) {
			NeedsGamut humanNeed = findAllNeedsIntensHuman(human);
			humanMapNeeds.put(human, humanNeed);
		}
		return humanMapNeeds;
	}
	private NeedsGamut findAllNeedsIntensHuman(Human human) {
		NeedsGamut societalNeed = new NeedsGamut();
		
		double hungerScore = 2.0 - human.nutrition / human.maxNutrition;
		societalNeed.addEmotion(NeedsGamut.EAT, hungerScore);
	
		//double thirstScore = 1.0 - human.hydration / human.maxHydration;
		//MathUti.addNumMap(societalNeed, "Drink", thirstScore);
		
		int shelterScore = 0;
		for (LocalGridLandClaim claim: human.allClaims) {
			//TODO
			//Calculate shelter score based on number of claims and amount of covered blocks
			
			/*
			LocalTile tile = grid.getTile(coords);
			if (tile != null) {
				LocalTile aboveTile = grid.getTile(coords);
				if (ItemData.isPlaceable(tile.tileBlockId) || 
						(aboveTile != null && ItemData.isPlaceable(aboveTile.tileFloorId)) ||
						(aboveTile != null && ItemData.isPlaceable(aboveTile.tileBlockId))) {
					shelterScore++;
				}
			}
			*/
		}
		
		int minShelter = 20;
		double normShelterScore = (double) Math.max(minShelter - shelterScore, 0) / minShelter;
		societalNeed.addEmotion(NeedsGamut.SHELTER, normShelterScore);
		
		societalNeed.addEmotion(NeedsGamut.CLOTHING, 1.0);
		
		if (human.home == null) {
			societalNeed.addEmotion(NeedsGamut.PERSONAL_HOME, 1.0);
			societalNeed.addEmotion(NeedsGamut.FURNITURE, 0.8);
		}
		else {
			double normFurnitureScore = 0;
			//Collection<Vector3i> buildingSpaces = human.home.calculatedLocations;
			for (int index = 0; index < human.home.buildingBlockIds.size(); index++) {
				int id = human.home.buildingBlockIds.get(index);
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
			normFurnitureScore = Math.min(normFurnitureScore, 1.0);
			societalNeed.addEmotion(NeedsGamut.PERSONAL_HOME, normFurnitureScore);
			societalNeed.addEmotion(NeedsGamut.FURNITURE, normFurnitureScore / 3);
		}	
		
		//double beautyScore = this.primaryGrid.averageBeauty(human.location.coords);
		//societalNeed.addEmotion(NeedsGamut.BEAUTY, 1.0 - beautyScore);

		societalNeed.addEmotion(NeedsGamut.SOLDIER, 0.1);
		
		//Compute social and sleep needs ("REST")
		double restScore = 2.0 - human.rest / human.maxRest;
		societalNeed.addEmotion(NeedsGamut.REST, restScore);

		return societalNeed;
	}
	
	/**
	 * @return Find a map of the direct utility generated by item
	 */
	private Map<String, Double> findUtilityByNeed(Integer itemId) {
		Map<String, Double> rawUtilByNeed = new HashMap<>();
		
		if (ItemData.isPlaceable(itemId)) {
			int pickupTime = ItemData.getPickupTime(itemId);
			MapUtil.addNumMap(rawUtilByNeed, "Shelter", pickupTime / 100.0);
		}
		
		List<ProcessStep> itemActions = ItemData.getItemActions(itemId);
		if (itemActions != null) {
			for (ProcessStep step: itemActions) {
				MapUtil.addNumMap(rawUtilByNeed, step.stepType, step.modifier);
			}
		}
		
		List<ProcessStep> itemProps = ItemData.getItemProps(itemId);
		if (itemProps != null) {
			for (ProcessStep step: itemProps) {
				MapUtil.addNumMap(rawUtilByNeed, step.stepType, step.modifier);
			}
		}
		
		double baseValue = ItemData.getBaseItemValue(itemId);
		MapUtil.addNumMap(rawUtilByNeed, "Wealth", baseValue);
		
		double beautyValue = ItemData.getItemBeautyValue(itemId);
		MapUtil.addNumMap(rawUtilByNeed, "Beauty", beautyValue / 3.0);
		
		/*
		CustomLog.outPrintln(ItemData.getNameFromId(itemId) + " stats: ");
		CustomLog.outPrintln(rawUtilByNeed);
		CustomLog.outPrintln("---------------------");
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
