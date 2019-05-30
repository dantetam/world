package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.dantetam.toolbox.VecGridUtil;
import io.github.dantetam.toolbox.MathUti;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.ai.Pathfinder;
import io.github.dantetam.world.ai.Pathfinder.ScoredPath;
import io.github.dantetam.world.civhumansocietyai.EmergentSocietyCalc;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ProcessData;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.process.LocalProcess;
import io.github.dantetam.world.process.LocalProcess.ProcessStep;
import io.github.dantetam.world.process.priority.BuildingHarvestPriority;
import io.github.dantetam.world.process.priority.BuildingPlacePriority;
import io.github.dantetam.world.process.priority.ConstructRoomPriority;
import io.github.dantetam.world.process.priority.DonePriority;
import io.github.dantetam.world.process.priority.EatPriority;
import io.github.dantetam.world.process.priority.ImpossiblePriority;
import io.github.dantetam.world.process.priority.ItemDeliveryBuildingPriority;
import io.github.dantetam.world.process.priority.ItemDeliveryPriority;
import io.github.dantetam.world.process.priority.ItemGroupPickupPriority;
import io.github.dantetam.world.process.priority.ItemPickupBuildingPriority;
import io.github.dantetam.world.process.priority.ItemPickupPriority;
import io.github.dantetam.world.process.priority.MovePriority;
import io.github.dantetam.world.process.priority.PatrolPriority;
import io.github.dantetam.world.process.priority.Priority;
import io.github.dantetam.world.process.priority.RestPriority;
import io.github.dantetam.world.process.priority.SoldierPriority;
import io.github.dantetam.world.process.priority.TileHarvestPriority;
import io.github.dantetam.world.process.priority.TilePlacePriority;
import io.github.dantetam.world.process.priority.WaitPriority;
import io.github.dantetam.world.process.prioritytask.DropoffInvTask;
import io.github.dantetam.world.process.prioritytask.HarvestBlockTileTask;
import io.github.dantetam.world.process.prioritytask.HarvestBuildingTask;
import io.github.dantetam.world.process.prioritytask.MoveTask;
import io.github.dantetam.world.process.prioritytask.PickupTask;
import io.github.dantetam.world.process.prioritytask.Task;
import kdtreegeo.KdTree;

public class LocalGridTimeExecution {
	
	public static Map<Integer, Double> calcUtility;
	public static Map<String, Double> groupItemRarity;
	public static List<Vector3i> importantLocations;
	
	public static void tick(WorldGrid world, LocalGrid grid, Society society) {
		calcUtility = society.findCompleteUtilityAllItems(null);
		groupItemRarity = society.findRawGroupsResRarity(null);
		importantLocations = society.getImportantLocations(society.societyCenter);
		
		System.out.println("<<<<>>>> Date: " + world.getTime());
		if (world.getTime().getSeconds() == 0) {
			society.createJobOffers();
			assignAllHumanJobs(society);
		}
		
		System.out.println("################");
		for (Human human: society.getAllPeople()) {
			String processName = human.processProgress == null ? "null" : human.processProgress.name;
			System.out.println(human.name + " located at " + human.location.coords + 
					", with process: " + processName); 
			System.out.println("Inventory (counts): " + human.inventory.toUniqueItemsMap());
					// + human.processProgress == null ? "null" : human.processProgress.name);
			System.out.println("Priority: " + human.activePriority);
			
			if (human.currentQueueTasks != null && human.currentQueueTasks.size() > 0) {
				Task task = human.currentQueueTasks.get(0);
				System.out.println(human.name + " has " + human.currentQueueTasks.size() + " tasks."); 
				if (task.taskTime <= 0) {
					System.out.println(human.name + " FINISHED task: " + task.getClass().getSimpleName());
					human.currentQueueTasks.remove(0);
					executeTask(grid, human, task);
					if (human.currentQueueTasks.size() == 0) {
						human.activePriority = null;
					}
				}
				else {
					task.taskTime--;
					System.out.println(human.name + " task in progress: " 
							+ task.getClass().getSimpleName() + " (" + task.taskTime + ")");
				}
			}
			if (human.activePriority != null && //Assign tasks if there are none (do not overwrite existing tasks)
					(human.currentQueueTasks == null || human.currentQueueTasks.size() == 0)) {
				human.currentQueueTasks = getTasksFromPriority(grid, human, human.activePriority);
				if (human.currentQueueTasks != null && human.currentQueueTasks.size() > 0)
					System.out.println(human.currentQueueTasks.get(0).getClass());
				if (human.currentQueueTasks == null) {
					human.currentQueueTasks = null;
				}
			}
			
			if (human.jobProcessProgress != null) {
				LocalProcess process = human.jobProcessProgress.jobWorkProcess;
				List<LivingEntity> capitalOwners = new ArrayList<LivingEntity>() {{
					add(human); 
					if (human.household != null) {
						addAll(human.household.householdMembers);
					}
					add(human.jobProcessProgress.boss);
				}};
				//Assign the location of the process where the steps are undertaken
				if (process.requiredBuildNameOrGroup != null && human.processBuilding == null) {
					assignBuilding(grid, human, capitalOwners, process.requiredBuildNameOrGroup);
				}
				else if (process.requiredTileNameOrGroup != null && human.processTile == null) {
					assignTile(grid, human, capitalOwners, process.requiredTileNameOrGroup);
				}
				
				//Follow through with one step, deconstruct into a priority, and get its status back.
				if (process.processSteps.size() > 0) {
					ProcessStep step = process.processSteps.get(0);
					if (human.activePriority == null) {
						human.activePriority = getPriorityForStep(society, grid, human, process, step);
						String priorityName = human.activePriority == null ? "null" : human.activePriority.getClass().getSimpleName();
						System.out.println(human.name + ", for its process: " + process + ", ");
								System.out.println("was given the PRIORITY: " + priorityName);
					}
					if (human.activePriority instanceof DonePriority || 
							human.activePriority instanceof ImpossiblePriority) {
						process.processSteps.remove(0);
						human.activePriority = null;
					}
					if (human.activePriority instanceof WaitPriority) {
						human.activePriority = null;
					}
				}
				//Process has been completed, remove it from person
				if (process.processSteps.size() == 0) { 
					executeProcessResActions(grid, human, process.processResActions);
					System.out.println(human.name + " COMPLETED process (as an employee for a job): " + process);
					human.jobProcessProgress = null; //Reset all jobs and job fields to null
					if (human.processBuilding != null) {
						human.processBuilding.currentUser = null;
						human.processBuilding = null;
					}
					if (human.processTile != null) {
						human.processTile.harvestInUse = false;
						human.processTile = null;
					}
				}
			}
			else if (human.processProgress != null) { 
				List<LivingEntity> capitalOwners = new ArrayList<LivingEntity>() {{
					add(human);
					if (human.household != null) {
						addAll(human.household.householdMembers);
					}
				}};
				//Assign the location of the process where the steps are undertaken
				if (human.processProgress.requiredBuildNameOrGroup != null && human.processBuilding == null) {
					assignBuilding(grid, human, capitalOwners, human.processProgress.requiredBuildNameOrGroup);
				}
				else if (human.processProgress.requiredTileNameOrGroup != null && human.processTile == null) {
					assignTile(grid, human, capitalOwners, human.processProgress.requiredTileNameOrGroup);
				}
				
				//Follow through with one step, deconstruct into a priority, and get its status back.
				if (human.processProgress.processSteps.size() > 0) {
					ProcessStep step = human.processProgress.processSteps.get(0);
					if (human.activePriority == null) {
						human.activePriority = getPriorityForStep(society, grid, human, human.processProgress, step);
						String priorityName = human.activePriority == null ? "null" : human.activePriority.getClass().getSimpleName();
						System.out.println(human.name + ", for its process: " + human.processProgress + ", ");
								System.out.println("was given the PRIORITY: " + priorityName);
					}
					if (human.activePriority instanceof DonePriority || 
							human.activePriority instanceof ImpossiblePriority) {
						human.processProgress.processSteps.remove(0);
						human.activePriority = null;
					}
					if (human.activePriority instanceof WaitPriority) {
						human.activePriority = null;
					}
				}
				//Process has been completed, remove it from person
				if (human.processProgress.processSteps.size() == 0) { 
					executeProcessResActions(grid, human, human.processProgress.processResActions);
					System.out.println(human.name + " COMPLETED process: " + human.processProgress);
					human.processProgress = null; //Reset all jobs and job fields to null
					if (human.processBuilding != null) {
						human.processBuilding.currentUser = null;
						human.processBuilding = null;
					}
					if (human.processTile != null) {
						human.processTile.harvestInUse = false;
						human.processTile = null;
					}
					assignSingleHumanJob(society, human);
				}
			}
			
			//Assign a new process. Note that a human may have a priority not linked to any higher structure,
			//which we do not want to override or chain incorrectly to a new job.
			if (human.processProgress == null && human.activePriority == null) {
				assignSingleHumanJob(society, human);
			}
			System.out.println();
		}
	}
	
	private static LocalBuilding assignBuilding(LocalGrid grid, LivingEntity being, 
			List<LivingEntity> validOwners, String buildingName) {
		int id = ItemData.getIdFromName(buildingName);
		KdTree<Vector3i> nearestItemsTree = grid.getKdTreeForItemId(id);
		if (nearestItemsTree == null || nearestItemsTree.size() == 0) return null;
		int numCandidates = Math.min(nearestItemsTree.size(), 10);
		Collection<Vector3i> nearestCoords = nearestItemsTree.nearestNeighbourListSearch(
				numCandidates, being.location.coords);
		for (Vector3i nearestCoord: nearestCoords) {
			LocalBuilding building = grid.getTile(nearestCoord).building;
			if (building.owner == null || validOwners.contains(building.owner)) {
				if (building.currentUser == null) {
					building.currentUser = being;
					being.processBuilding = building;
					break;
				}
			}
		}
		return being.processBuilding;
	}
	
	private static LocalTile assignTile(LocalGrid grid, LivingEntity being, 
			List<LivingEntity> validOwners, String tileName) {
		int tileId = ItemData.getIdFromName(tileName);
		KdTree<Vector3i> items = grid.getKdTreeForTile(tileId);
		if (items == null) return null;
		Collection<Vector3i> candidates = items.nearestNeighbourListSearch(10, being.location.coords);
		Map<LocalTile, Double> tileByPathScore = new HashMap<>();
		for (Vector3i candidate: candidates) {
			System.out.println("Calc path: " + being.location.coords + " to -> " + grid.getTile(candidate).coords);
			LocalTile tile = grid.getTile(candidate);
			if (tile.humanClaim == null || validOwners.contains(tile.humanClaim)) {
				if (!tile.harvestInUse) {
					ScoredPath scoredPath = new Pathfinder(grid).findPath(being, being.location, grid.getTile(candidate));
					tileByPathScore.put(tile, scoredPath.score);
				}
			}
		}
		tileByPathScore = MathUti.getSortedMapByValueDesc(tileByPathScore);
		for (Object obj: tileByPathScore.keySet().toArray()) {
			LocalTile bestTile = (LocalTile) obj;
			being.processTile = bestTile;
			bestTile.harvestInUse = true;
			return bestTile;
		}
		return null;
	}
	
	private static List<Task> getTasksFromPriority(LocalGrid grid, LivingEntity being, Priority priority) {
		List<Task> tasks = new ArrayList<>();
		if (priority instanceof ItemPickupPriority) {
			ItemPickupPriority itemPriority = (ItemPickupPriority) priority;
			if (itemPriority.coords.manhattanDist(being.location.coords) <= 1) {
				grid.getTile(itemPriority.coords).itemsOnFloor.subtractItem(itemPriority.item);
				being.inventory.addItem(itemPriority.item);
				grid.removeItemRecordToWorld(itemPriority.coords, itemPriority.item);
				return null;
			}
			else {
				return getTasksFromPriority(grid, being, new MovePriority(itemPriority.coords));
			}
		}
		else if (priority instanceof ItemDeliveryPriority) {
			ItemDeliveryPriority itemPriority = (ItemDeliveryPriority) priority;
			if (itemPriority.coords.manhattanDist(being.location.coords) <= 1) {
				List<InventoryItem> desiredItems = itemPriority.inventory.getItems();
				being.inventory.subtractItems(desiredItems);
				grid.getTile(itemPriority.coords).itemsOnFloor.addItems(desiredItems);
				return null;
			}
			else {
				return getTasksFromPriority(grid, being, new MovePriority(itemPriority.coords));
			}
		}
		else if (priority instanceof ItemPickupBuildingPriority) {
			ItemPickupBuildingPriority itemPriority = (ItemPickupBuildingPriority) priority;
			if (itemPriority.coords.manhattanDist(being.location.coords) <= 1) {
				grid.getTile(itemPriority.coords).building.inventory.subtractItem(itemPriority.item);
				being.inventory.addItem(itemPriority.item);
				
				grid.removeItemRecordToWorld(itemPriority.coords, itemPriority.item);
				return null;
			}
			else {
				return getTasksFromPriority(grid, being, new MovePriority(itemPriority.coords));
			}
		}
		else if (priority instanceof ItemDeliveryBuildingPriority) {
			ItemDeliveryBuildingPriority itemPriority = (ItemDeliveryBuildingPriority) priority;
			if (itemPriority.coords.manhattanDist(being.location.coords) <= 1) {
				List<InventoryItem> desiredItems = itemPriority.inventory.getItems();
				being.inventory.subtractItems(desiredItems);
				grid.getTile(itemPriority.coords).building.inventory.addItems(desiredItems);
				
				grid.addItemRecordsToWorld(itemPriority.coords, desiredItems);
				return null;
			}
			else {
				return getTasksFromPriority(grid, being, new MovePriority(itemPriority.coords));
			}
		}
		else if (priority instanceof TileHarvestPriority) {
			TileHarvestPriority tilePriority = (TileHarvestPriority) priority;
			if (tilePriority.coords.manhattanDist(being.location.coords) <= 1) {
				int pickupTime = ItemData.getPickupTime(grid.getTile(tilePriority.coords).tileBlockId);
				tasks.add(new HarvestBlockTileTask(pickupTime, tilePriority.coords));
			}
			else {
				return getTasksFromPriority(grid, being, new MovePriority(tilePriority.coords));
			}
		}
		else if (priority instanceof BuildingPlacePriority) {
			BuildingPlacePriority buildPriority = (BuildingPlacePriority) priority;
			
			if (buildPriority.coords.manhattanDist(being.location.coords) <= 1) {
				LocalTile tile = grid.getTile(buildPriority.coords);
				if (being.inventory.hasItem(buildPriority.buildingItem) &&
						tile.building == null) {
					being.inventory.subtractItem(buildPriority.buildingItem);
					LocalBuilding newBuilding = ItemData.building(buildPriority.buildingItem.itemId);
					grid.addBuilding(newBuilding, buildPriority.coords, false);
					return null;
				}
			}
			else {
				return getTasksFromPriority(grid, being, new MovePriority(buildPriority.coords));
			}
		}
		else if (priority instanceof BuildingHarvestPriority) {
			BuildingHarvestPriority buildPriority = (BuildingHarvestPriority) priority;
			
			if (buildPriority.coords.manhattanDist(being.location.coords) <= 1) {
				LocalTile tile = grid.getTile(buildPriority.coords);
				int pickupTime = ItemData.getPickupTime(tile.tileBlockId);
				tasks.add(new HarvestBuildingTask(pickupTime, buildPriority.coords, tile.building));
			}
			else {
				return getTasksFromPriority(grid, being, new MovePriority(buildPriority.coords));
			}
		}
		else if (priority instanceof EatPriority) {
			return getTasksFromPriority(grid, being, 
					progressToFindItemGroup(grid, being.location.coords, "Food", 1));
		}
		else if (priority instanceof RestPriority) {
			return getTasksFromPriority(grid, being, 
					progressToFindItemGroup(grid, being.location.coords, "Bed", 1));
		}
		else if (priority instanceof ConstructRoomPriority) {
			ConstructRoomPriority consPriority = (ConstructRoomPriority) priority;
			if (consPriority.remainingBuildCoords.size() == 0) {
				grid.setInUseRoomSpace(consPriority.allBuildingCoords, true);
				return null;
			}
			
			Vector3i bestLocation = null;
			while (bestLocation == null || grid.getTile(bestLocation).tileFloorId == ItemData.ITEM_EMPTY_ID) {
				bestLocation = consPriority.remainingBuildCoords.remove(0);
			}
			
			Collection<Integer> rankedMaterials = consPriority.rankedBuildMaterials;
			for (int materialId: rankedMaterials) {
				if (being.inventory.findItemCount(materialId) > 0) {
					return getTasksFromPriority(grid, being, new TilePlacePriority(bestLocation, materialId));
				}
			}
			return null;
		}
		else if (priority instanceof MovePriority) {
			MovePriority movePriority = (MovePriority) priority;
			ScoredPath scoredPath = new Pathfinder(grid).findPath(being, being.location, grid.getTile(movePriority.coords));
			List<LocalTile> path = scoredPath.path;
			for (int i = 0; i < path.size() - 1; i++) {
				Vector3i coordsFrom = path.get(i).coords;
				Vector3i coordsTo = path.get(i+1).coords;
				tasks.add(new MoveTask(1, coordsFrom, coordsTo));
			}
		}
		else if (priority instanceof SoldierPriority) {
			//SoldierPriority soldierPriority = (SoldierPriority) priority;
			
			double weaponAmt = groupItemRarity.containsKey("Weapon") ? groupItemRarity.get("Weapon") : 0;
			double armorAmt = groupItemRarity.containsKey("Armor") ? groupItemRarity.get("Armor") : 0;
			
			double weaponReq = SoldierPriority.weaponAmtRequired(grid, being);
			double armorReq = SoldierPriority.armorAmtRequired(grid, being);
			
			Priority resultPriority = null;
			
			if (armorReq > 0 && armorAmt > 0) {
				resultPriority = progressToFindItemGroup(grid, being.location.coords, "Armor", 1);
				if (!(resultPriority instanceof ImpossiblePriority)) {
					return getTasksFromPriority(grid, being, resultPriority);
				}
			}
			if (weaponReq > 0 && weaponAmt > 0) {
				resultPriority = progressToFindItemGroup(grid, being.location.coords, "Weapon", 1);
				if (!(resultPriority instanceof ImpossiblePriority)) {
					return getTasksFromPriority(grid, being, resultPriority);
				}
			}
			
			resultPriority = new PatrolPriority(importantLocations);
			return getTasksFromPriority(grid, being, resultPriority);
		}
		else if (priority instanceof PatrolPriority) {
			PatrolPriority patrolPriority = (PatrolPriority) priority;
			List<Vector3i> locations = patrolPriority.locations;
			if (locations.size() == 0) {
				return tasks;
			}
			else {
				int randIndex = (int) (Math.random() * locations.size());
				Vector3i randomLocation = locations.get(randIndex);
				if (randomLocation.manhattanDist(being.location.coords) <= 1) {
					return null;
				}
				else {
					return getTasksFromPriority(grid, being, new MovePriority(randomLocation));
				}
			}
		}
		else if (priority instanceof DonePriority) {
			return null;
		}
		return tasks;
	}
	
	/**
	 * Deconstruct a process into a priority.
	 * @return the Priority object which represents the given group of actions/approach, for working towards
	 * 		the given process and step.
	 */
	private static Priority getPriorityForStep(Society society, LocalGrid grid, Human being, 
			LocalProcess process, ProcessStep step) {
		Priority priority = null;
		
		System.out.println("Figuring out priority, for process: " + process);
		
		if (process.name.equals("Build Basic Home")) {
			int width = (int) Math.ceil(Math.sqrt(being.ownedBuildings.size() + being.inventory.size() + 8));
			Vector2i requiredSpace = new Vector2i(width, width);
			
			List<Vector3i> bestRectangle = findBestOpenRectSpace(grid, being.location.coords, requiredSpace);
			List<Vector3i> borderRegion = VecGridUtil.getBorderRegionFromCoords(bestRectangle);
			
			Set<Integer> bestBuildingMaterials = society.getBestBuildingMaterials(calcUtility, 
					being, borderRegion.size());
				
			if (bestRectangle != null) {
				priority = new ConstructRoomPriority(borderRegion, bestBuildingMaterials);
			}
			else {
				priority = new ImpossiblePriority();
			}
			return priority;
		}
		else if (process.name.equals("Local Soldier Duty")) {
			return new SoldierPriority(society.societyCenter, being);
		}
		
		Vector3i primaryLocation = null;
		Inventory destinationInventory = null;
		String itemMode = "";
		if (being.processBuilding != null) {
			primaryLocation = being.processBuilding.getPrimaryLocation();
			destinationInventory = being.processBuilding.inventory;
			itemMode = "Building";
		}
		else if (being.processTile != null) {
			primaryLocation = being.processTile.coords;
			destinationInventory = being.inventory;
			itemMode = "Tile";
		}
		else if (being.processProgress.requiredBuildNameOrGroup == null) {
			primaryLocation = being.location.coords;
			destinationInventory = being.inventory;
			itemMode = "Personal";
		}
		
		if ((primaryLocation == null && being.processProgress.requiredBuildNameOrGroup != null)
				|| being.processProgress.isCreatedAtSite) {
			System.out.println("Find status building case");
			System.out.println(being.processProgress.requiredBuildNameOrGroup);
			System.out.println(being.processProgress);
			
			int buildingId; 
			Vector2i requiredSpace;
			
			if (being.processProgress.requiredBuildNameOrGroup != null) {
				buildingId = ItemData.getIdFromName(being.processProgress.requiredBuildNameOrGroup);
				requiredSpace = ItemData.buildingSize(buildingId);
			}
			else {
				buildingId = -1;
				requiredSpace = new Vector2i(1, 1);
			}
			
			//If available building, use it, 
			//otherwise find space needed for building, allocate it, and start allocating a room
			Object[] boundsData = grid.getNearestViableRoom(being.location.coords, requiredSpace);

			if (boundsData == null) { //No available rooms to use
				List<Vector3i> bestRectangle = findBestOpenRectSpace(grid, being.location.coords, requiredSpace);
			
				if (bestRectangle != null) {
					System.out.println("Create building space: " + Arrays.toString(VecGridUtil.findCoordBounds(bestRectangle)));
					Set<Integer> bestBuildingMaterials = society.getBestBuildingMaterials(calcUtility, 
							being, (requiredSpace.x + requiredSpace.y) * 2);
					grid.setInUseRoomSpace(bestRectangle, true);
					priority = new ConstructRoomPriority(bestRectangle, bestBuildingMaterials);
				}
				else {
					System.out.println("Could not create building space of size: " + requiredSpace);
					priority = new ImpossiblePriority();
				}
				return priority;
			}
			else {
				Vector3i nearOpenSpace = (Vector3i) boundsData[0]; // = grid.getNearOpenSpace(society.societyCenter);
				Vector2i bounds2d = (Vector2i) boundsData[1];
				
				System.out.println("Assigned building space: " + nearOpenSpace.toString());
				grid.setInUseRoomSpace(nearOpenSpace, bounds2d, true);
				
				if (being.processProgress.isCreatedAtSite) {
					being.processTile = grid.getTile(nearOpenSpace);
					if (being.location.coords.manhattanDist(primaryLocation) <= 1) {
						//continue
					}
					else {
						return new MovePriority(primaryLocation);
					}
				}
				else {
					priority = new BuildingPlacePriority(nearOpenSpace, ItemData.createItem(buildingId, 1));
					return priority;
				}
			}
		}
		
		if (step.stepType.equals("I")) {
			if (being.processBuilding != null && being.processBuilding.inventory.hasItems(process.inputItems)) {
				being.processBuilding.inventory.subtractItems(process.inputItems);
				return new DonePriority();
			}
			else if (destinationInventory.hasItems(process.inputItems) && itemMode.equals("Personal")) {
				being.inventory.subtractItems(process.inputItems);
				return new DonePriority();
			}
			else if (destinationInventory.hasItems(process.inputItems) && being.processTile != null) {
				//priority = new ItemDeliveryPriority(primaryLocation, new Inventory(process.inputItems));
				if (being.location.coords.manhattanDist(primaryLocation) <= 1) {
					return new DonePriority();
				}
				else {
					priority = new MovePriority(primaryLocation);
				}
			}
			else {
				Object[] invData = being.inventory.findRemainingItemsNeeded(process.inputItems);
				Map<Integer, Integer> regularItemNeeds = (Map) invData[0];
				
				System.out.println("Searching for: " + regularItemNeeds);
				
				int firstItemNeeded = (Integer) regularItemNeeds.keySet().toArray()[0];
				int amountNeeded = regularItemNeeds.get(firstItemNeeded);
				
				return progressToFindItem(grid, being.location.coords, firstItemNeeded, amountNeeded);
			}
		}
		else if (step.stepType.startsWith("S")) {
			if (being.location.coords.manhattanDist(primaryLocation) <= 1) {
				step.timeTicks--;
				if (step.timeTicks <= 0) {
					return new DonePriority();
				}
				else {
					priority = new WaitPriority();
				}
			}
			else {
				priority = new MovePriority(primaryLocation);
			}
		}
		else if (step.stepType.equals("HBuilding")) {
			LocalTile tile = grid.getTile(primaryLocation);
			if (tile.building == null) {
				return new DonePriority();
			}
			if (being.location.coords.manhattanDist(primaryLocation) <= 1) {
				priority = new BuildingHarvestPriority(tile.coords, tile.building);
			}
			else {
				priority = new MovePriority(primaryLocation);
			}
		}
		else if (step.stepType.equals("HTile")) {
			LocalTile tile = grid.getTile(primaryLocation);
			if (tile.tileBlockId == ItemData.ITEM_EMPTY_ID || step.timeTicks <= 0) {
				return new DonePriority();
			}
			if (being.location.coords.manhattanDist(primaryLocation) <= 1) {
				priority = new TileHarvestPriority(tile.coords);
			}
			else {
				priority = new MovePriority(primaryLocation);
			}
		}
		else if (step.stepType.startsWith("Wait")) {
			step.timeTicks--;
			if (step.timeTicks <= 0) {
				return new DonePriority();
			}
			else {
				return new WaitPriority();
			}
		}
		else if (step.stepType.startsWith("U")) {
			step.timeTicks--;
			if (step.timeTicks <= 0) {
				return new DonePriority();
			}
			else {
				
			}
		}
		else if (step.stepType.equals("O")) {
			List<InventoryItem> outputItems = process.outputItems.getOneItemDrop();
			System.out.println("Dropped items: " + new Inventory(outputItems) + " at place: " + itemMode);
			destinationInventory.addItems(outputItems);
			if (!itemMode.equals("Personal")) { //Keep a record of this item in the 3d space
				for (InventoryItem item: outputItems) {
					grid.addItemRecordToWorld(primaryLocation, item);
				}
			}
			return new DonePriority();
		}
		
		if (!step.stepType.startsWith("U") && priority == null)
			System.err.println("Warning, case not covered for priority from step, returning null priority");
		return priority;
	}
	
	private static void executeProcessResActions(LocalGrid grid, LivingEntity being, List<ProcessStep> actions) {
		for (ProcessStep action: actions) {
			executeProcessResAction(grid, being, action);
		}
	}
	private static void executeProcessResAction(LocalGrid grid, LivingEntity being, ProcessStep action) {
		if (action.stepType.equals("Eat")) {
			being.feed(action.modifier);
		}
		else if (action.stepType.equals("Rest")) {
			being.rest(action.modifier);
		}
		else if (action.stepType.equals("Heal")) {
			//TODO
		}
	}
	
	private static void executeTask(LocalGrid grid, LivingEntity being, Task task) {
		if (task instanceof MoveTask) {
			MoveTask moveTask = (MoveTask) task;
			grid.movePerson(being, grid.getTile(moveTask.coordsFrom));
		}
		else if (task instanceof PickupTask) {
			PickupTask pickupTask = (PickupTask) task;
		}
		else if (task instanceof DropoffInvTask) {
			DropoffInvTask dropTask = (DropoffInvTask) task;
		}
		else if (task instanceof HarvestBlockTileTask) {
			HarvestBlockTileTask harvestTileTask = (HarvestBlockTileTask) task;
			grid.putBlockIntoTile(harvestTileTask.pickupCoords, ItemData.ITEM_EMPTY_ID);
		}
		else if (task instanceof HarvestBuildingTask) {
			HarvestBuildingTask harvestBuildTask = (HarvestBuildingTask) task;
			grid.removeBuilding(harvestBuildTask.buildingTarget);
		}
	}
	
	private static void assignAllHumanJobs(Society society) {
		List<Human> humans = society.getAllPeople();
		for (Human human: humans) {
			assignSingleHumanJob(society, human);
		}
	}
	private static void assignSingleHumanJob(Society society, Human human) {
		Map<Integer, Double> calcUtility = society.findCompleteUtilityAllItems(human);
		Map<LocalProcess, Double> bestProcesses = society.prioritizeProcesses(calcUtility, human, 20, null);
		LocalProcess randBiasedChosenProcess = MathUti.randChoiceFromWeightMap(bestProcesses);
		human.processProgress = randBiasedChosenProcess;
	}
	
	private static void collectivelyAssignJobsSociety(Society society) {
		Map<Integer, Double> calcUtility = society.findCompleteUtilityAllItems(null);
		Map<LocalProcess, Double> bestProcesses = society.prioritizeProcesses(calcUtility, null, 20, null);
		List<Human> humans = society.getAllPeople();
		
		int humanIndex = 0;
		for (Entry<LocalProcess, Double> entry: bestProcesses.entrySet()) {
			int numPeople = (int) (entry.getValue() * humans.size());
			numPeople = Math.max(1, numPeople);
			
			while (numPeople > 0 && humanIndex < numPeople) {
				Human human = humans.get(humanIndex);
				if (human.processProgress == null) {
					human.processProgress = entry.getKey().clone();
					numPeople--;
				}
				humanIndex++;
			}
		}
	}
	
	private static Priority progressToFindItem(LocalGrid grid, Vector3i centerCoords,
			int firstItemNeeded, int amountNeeded) {
		KdTree<Vector3i> nearestItemsTree = grid.getKdTreeForItemId(firstItemNeeded);
		
		if (nearestItemsTree == null) {
			List<LocalProcess> outputItemProcesses = ProcessData.getProcessesByOutput(firstItemNeeded);
			Vector3i bestLocation = null;
			double bestScore = 0;
			for (LocalProcess process: outputItemProcesses) {
				if (process.name.startsWith("Harvest ")) {
					int inputTileId = ItemData.getIdFromName(process.requiredTileNameOrGroup);
					double pickupTime = ItemData.getPickupTime(inputTileId);
					double expectedNumItem = process.outputItems.itemExpectation().get(inputTileId);
					
					KdTree<Vector3i> itemTree = grid.getKdTreeForTile(inputTileId);
					int numCandidates = Math.min(itemTree.size(), 10);
					Collection<Vector3i> nearestCoords = itemTree.nearestNeighbourListSearch(
							numCandidates, centerCoords);
					for (Vector3i itemCoords: nearestCoords) {
						int distUtil = centerCoords.manhattanDist(itemCoords);
						if (bestLocation == null || distUtil < bestScore) {
							bestLocation = itemCoords;
							bestScore = distUtil * expectedNumItem / pickupTime;
						}
					}
				}
			}
			if (bestLocation != null) {
				return new TileHarvestPriority(bestLocation);
			}
			else {
				return new ImpossiblePriority();
			}
		}
		
		int numCandidates = Math.min(nearestItemsTree.size(), 10);
		Collection<Vector3i> nearestCoords = nearestItemsTree.nearestNeighbourListSearch(
				numCandidates, centerCoords);
	
		Map<Vector3i, Double> score = new HashMap<>();
		for (Vector3i itemCoords: nearestCoords) {
			int numItemsAtTile = grid.getTile(itemCoords).itemsOnFloor.findItemCount(firstItemNeeded);
			numItemsAtTile += grid.getTile(itemCoords).building.inventory.findItemCount(firstItemNeeded);
			int distUtil = centerCoords.manhattanDist(itemCoords);
			score.put(itemCoords, Math.min(numItemsAtTile, amountNeeded) / Math.pow(distUtil, 2));
		}
		score = MathUti.getSortedMapByValueDesc(score);
		
		Vector3i location = (Vector3i) score.keySet().toArray()[0];
		InventoryItem itemClone = new InventoryItem(firstItemNeeded, amountNeeded, ItemData.getNameFromId(firstItemNeeded));
		
		return new ItemPickupPriority(location, itemClone);
	}
	
	private static Priority progressToFindItemGroup(LocalGrid grid, Vector3i centerCoords,
			String itemGroupName, int amountNeeded) { // Function<LocalTile, Double> scoringMetric
		KdTree<Vector3i> nearestItemsTree = grid.getKdTreeForItemGroup(itemGroupName);
		
		if (nearestItemsTree == null) {
			Set<Integer> groupIds = ItemData.getGroupIds(itemGroupName);
			Vector3i bestLocation = null;
			double bestScore = 0;
			for (int groupId: groupIds) {
				List<LocalProcess> outputItemProcesses = ProcessData.getProcessesByOutput(groupId);
				for (LocalProcess process: outputItemProcesses) {
					if (process.name.startsWith("Harvest ")) {
						int inputTileId = ItemData.getIdFromName(process.requiredTileNameOrGroup);
						double pickupTime = ItemData.getPickupTime(inputTileId);
						double expectedNumItem = process.outputItems.itemExpectation().get(inputTileId);
						
						KdTree<Vector3i> itemTree = grid.getKdTreeForTile(inputTileId);
						int numCandidates = Math.min(itemTree.size(), 10);
						Collection<Vector3i> nearestCoords = itemTree.nearestNeighbourListSearch(
								numCandidates, centerCoords);
						for (Vector3i itemCoords: nearestCoords) {
							int distUtil = centerCoords.manhattanDist(itemCoords);
							if (bestLocation == null || distUtil < bestScore) {
								bestLocation = itemCoords;
								bestScore = distUtil * expectedNumItem / pickupTime;
							}
						}
					}
				}
			}
			if (bestLocation != null) {
				return new TileHarvestPriority(bestLocation);
			}
			else {
				return new ImpossiblePriority();
			}
		}
		
		int numCandidates = Math.min(nearestItemsTree.size(), 10);
		Collection<Vector3i> nearestCoords = nearestItemsTree.nearestNeighbourListSearch(
				numCandidates, centerCoords);
	
		Map<Vector3i, Double> score = new HashMap<>();
		for (Vector3i itemCoords: nearestCoords) {
			LocalTile tile = grid.getTile(itemCoords);
			int numItemsAtTile = tile.itemsOnFloor.findItemCountGroup(itemGroupName);
			numItemsAtTile += tile.building.inventory.findItemCountGroup(itemGroupName);
			int distUtil = centerCoords.manhattanDist(itemCoords);
			score.put(itemCoords, Math.min(numItemsAtTile, amountNeeded) / Math.pow(distUtil, 2));
		}
		score = MathUti.getSortedMapByValueDesc(score);		
		Vector3i location = (Vector3i) score.keySet().toArray()[0];

		return new ItemGroupPickupPriority(location, itemGroupName, amountNeeded);
	}
	
	private static List<Vector3i> findBestOpenRectSpace(LocalGrid grid, Vector3i coords, Vector2i requiredSpace) {
		Set<Vector3i> openSpace = SpaceFillingAlgorithm.findAvailableSpace(grid, coords, 
				requiredSpace.x + 2, requiredSpace.y + 2, true);
		
		if (openSpace != null) {
			int height = openSpace.iterator().next().z;
			int[] maxSubRect = VecGridUtil.findMaxRect(openSpace);
			int bestR = maxSubRect[0], bestC = maxSubRect[1],
					rectR = maxSubRect[2], rectC = maxSubRect[3];
				
			List<Vector3i> bestRectangle = Vector3i.getRange(
					new Vector3i(bestR, bestC, height), new Vector3i(bestR + rectR - 1, bestC + rectC - 1, height));
			Collections.sort(bestRectangle, new Comparator<Vector3i>() {
				@Override
				public int compare(Vector3i o1, Vector3i o2) {
					return o2.manhattanDist(coords) - o1.manhattanDist(coords);
				}
			});
			return bestRectangle; 
		}
		else {
			return null;
		}
	}
	
	/*
	private static Set<InventoryItem> lookForOwnedItems(Human human, Set<Integer> itemIds) {
		
	}
	
	private static Set<InventoryItem> findItemIdsToUse(Human human, Set<Integer> itemIds) {
		
	}
	*/
	
}
