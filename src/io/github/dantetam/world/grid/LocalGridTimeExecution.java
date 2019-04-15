package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.dantetam.toolbox.AlgUtil;
import io.github.dantetam.toolbox.MathUti;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.ai.Pathfinder;
import io.github.dantetam.world.ai.Pathfinder.ScoredPath;
import io.github.dantetam.world.civilization.Human;
import io.github.dantetam.world.civilization.LivingEntity;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ProcessData;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.Process;
import io.github.dantetam.world.process.Process.ProcessStep;
import io.github.dantetam.world.process.priority.BuildingPlacePriority;
import io.github.dantetam.world.process.priority.ConstructRoomPriority;
import io.github.dantetam.world.process.priority.DonePriority;
import io.github.dantetam.world.process.priority.ImpossiblePriority;
import io.github.dantetam.world.process.priority.ItemDeliveryBuildingPriority;
import io.github.dantetam.world.process.priority.ItemDeliveryPriority;
import io.github.dantetam.world.process.priority.ItemPickupBuildingPriority;
import io.github.dantetam.world.process.priority.ItemPickupPriority;
import io.github.dantetam.world.process.priority.MovePriority;
import io.github.dantetam.world.process.priority.Priority;
import io.github.dantetam.world.process.priority.TileHarvestPriority;
import io.github.dantetam.world.process.priority.TilePlacePriority;
import io.github.dantetam.world.process.prioritytask.DropoffInvTask;
import io.github.dantetam.world.process.prioritytask.MoveTask;
import io.github.dantetam.world.process.prioritytask.PickupTask;
import io.github.dantetam.world.process.prioritytask.Task;
import kdtreegeo.KdTree;

public class LocalGridTimeExecution {
	
	public static double numDayTicks = 0;
	public static Map<Integer, Double> calcUtility;
	
	public static void tick(LocalGrid grid, Society society) {
		calcUtility = society.findCompleteUtilityAllItems(null);
		
		System.out.println("<<<<<########>>>>> NUMBER DAY TICKS: " + numDayTicks);
		if (numDayTicks % 1440 == 0) {
			numDayTicks = 0;
			assignAllHumanJobs(society);
		}
		for (Human human: society.getAllPeople()) {
			System.out.println("################");
			String processName = human.processProgress == null ? "null" : human.processProgress.name;
			System.out.println(human.name + " is at location " + human.location.coords + 
					", with process: " + processName); 
			System.out.println("Inventory (counts): " + human.inventory.toUniqueItemsMap());
					// + human.processProgress == null ? "null" : human.processProgress.name);
			System.out.println("Priority: " + human.activePriority);
			
			if (human.currentQueueTasks != null && human.currentQueueTasks.size() > 0) {
				Task task = human.currentQueueTasks.get(0);
				if (task.taskTime <= 0) {
					System.out.println(human.name + " FINISHED task: " + task.getClass());
					human.currentQueueTasks.remove(0);
					executeTask(grid, human, task);
					if (human.currentQueueTasks.size() == 0) {
						human.activePriority = null;
					}
				}
				else {
					task.taskTime--;
					System.out.println(human.name + " made progress on task: " 
							+ task.getClass() + " (" + task.taskTime + ")");
				}
			}
			if (human.activePriority != null && //Assign tasks if there are none (do not overwrite existing tasks)
					(human.currentQueueTasks == null || human.currentQueueTasks.size() == 0)) {
				human.currentQueueTasks = getTasksFromPriority(grid, human, human.activePriority);
				if (human.currentQueueTasks.size() > 0)
					System.out.println(human.currentQueueTasks.get(0).getClass());
			}
			if (human.processProgress != null) { 
				//Assign the location of the process where the steps are undertaken
				if (human.processProgress.requiredBuildNameOrGroup != null && human.processBuilding == null) {
					assignBuilding(grid, human, human.processProgress.requiredBuildNameOrGroup);
				}
				else if (human.processProgress.requiredTileNameOrGroup != null) {
					assignTile(grid, human, human.processProgress.requiredTileNameOrGroup);
				}
				
				//Process has been completed, remove it from person
				if (human.processProgress.processSteps.size() == 0) { 
					System.out.println(human.name + " COMPLETED process: " + human.processProgress);
					human.processProgress = null;
					
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
				else {
					ProcessStep step = human.processProgress.processSteps.get(0);
					
					if (human.activePriority == null) {
						human.activePriority = getPriorityForStep(society, grid, human, human.processProgress, step);
					}
					
					String priorityName = human.activePriority == null ? "null" : human.activePriority.getClass().getName();
					System.out.println(human.name + ", for its process: " + human.processProgress + ", ");
							System.out.println("was given the PRIORITY: " + priorityName);
					if (human.activePriority instanceof DonePriority || 
							human.activePriority instanceof ImpossiblePriority) {
						human.processProgress.processSteps.remove(0);
						//human.processProgress = null;
						human.activePriority = null;
					}
				}
			}
			if (human.processProgress == null && human.activePriority == null) {
				assignSingleHumanJob(society, human);
			}
		}
		numDayTicks++;
	}
	
	private static LocalBuilding assignBuilding(LocalGrid grid, LivingEntity being, String buildingName) {
		int id = ItemData.getIdFromName(buildingName);
		KdTree<Vector3i> nearestItemsTree = grid.getKdTreeForItemId(id);
		int numCandidates = Math.min(nearestItemsTree.size(), 10);
		Collection<Vector3i> nearestCoords = nearestItemsTree.nearestNeighbourListSearch(
				numCandidates, being.location.coords);
		for (Vector3i nearestCoord: nearestCoords) {
			LocalBuilding building = grid.getTile(nearestCoord).building;
			if (building.currentUser == null) {
				building.currentUser = being;
				being.processBuilding = building;
				break;
			}
		}
		return being.processBuilding;
	}
	
	private static LocalTile assignTile(LocalGrid grid, LivingEntity being, String tileName) {
		int tileId = ItemData.getIdFromName(tileName);
		KdTree<Vector3i> items = grid.getKdTreeForTile(tileId);
		Collection<Vector3i> candidates = items.nearestNeighbourListSearch(10, being.location.coords);
		Map<LocalTile, Integer> tileByPathScore = new HashMap<>();
		for (Vector3i candidate: candidates) {
			LocalTile tile = grid.getTile(candidate);
			if (!tile.harvestInUse) {
				ScoredPath scoredPath = Pathfinder.findPath(grid, being, being.location, grid.getTile(candidate));
				tileByPathScore.put(tile, scoredPath.score);
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
				return tasks;
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
				grid.getTile(itemPriority.coords).building.inventory.addItems(desiredItems);
				return tasks;
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
				return tasks;
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
				return tasks;
			}
			else {
				return getTasksFromPriority(grid, being, new MovePriority(itemPriority.coords));
			}
		}
		else if (priority instanceof TileHarvestPriority) {
			TileHarvestPriority tilePriority = (TileHarvestPriority) priority;
			if (tilePriority.coords.manhattanDist(being.location.coords) <= 1) {
				grid.putBlockIntoTile(tilePriority.coords, ItemData.ITEM_EMPTY_ID);
				return tasks;
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
				}
			}
			else {
				return getTasksFromPriority(grid, being, new MovePriority(buildPriority.coords));
			}
		}
		else if (priority instanceof ConstructRoomPriority) {
			ConstructRoomPriority consPriority = (ConstructRoomPriority) priority;
			
			if (consPriority.allBuildingCoords.size() == 0) return null;
			
			Vector3i bestLocation = consPriority.allBuildingCoords.get(0);
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
			ScoredPath scoredPath = Pathfinder.findPath(grid, being, being.location, grid.getTile(movePriority.coords));
			List<LocalTile> path = scoredPath.path;
			for (int i = 0; i < path.size() - 1; i++) {
				Vector3i coordsFrom = path.get(i).coords;
				Vector3i coordsTo = path.get(i+1).coords;
				tasks.add(new MoveTask(1, coordsFrom, coordsTo));
			}
		}
		else if (priority instanceof DonePriority) {
			return null;
		}
		return tasks;
	}
	
	private static Priority getPriorityForStep(Society society, LocalGrid grid, Human being, 
			Process process, ProcessStep step) {
		Priority priority = null;
		
		System.out.println("Figuring out priority, for process: " + process);
		
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
			destinationInventory = being.processTile.itemsOnFloor;
			itemMode = "Tile";
		}
		else if (being.processProgress.requiredBuildNameOrGroup == null) {
			primaryLocation = being.location.coords;
			destinationInventory = being.inventory;
			itemMode = "Personal";
		}
		
		if (primaryLocation == null) {
			int buildingId = ItemData.getIdFromName(being.processProgress.requiredBuildNameOrGroup);
			Vector2i requiredSpace = ItemData.buildingSize(buildingId);
			
			Vector3i nearOpenSpace = null; // = grid.getNearOpenSpace(society.societyCenter);
			
			//If available building, use it, 
			//otherwise find space needed for building, allocate it, and start allocating a room
			Collection<Vector3i> nearestBuildingCoords = grid.getNearestBuildings(being.location.coords);
			for (Vector3i nearestBuildingCoord: nearestBuildingCoords) {
				LocalBuilding building = grid.getTile(nearestBuildingCoord).building;
				if (building != null) {
					Set<Vector3i> emptySpace = grid.getFreeSpace(building);
					if (emptySpace.size() > requiredSpace.x * requiredSpace.y) {
						int height = emptySpace.iterator().next().z;
						int[] bestBounds = AlgUtil.findMaxRect(emptySpace);
						nearOpenSpace = new Vector3i(bestBounds[0], bestBounds[1], height);
						break;
					}
				}
			}
			if (nearOpenSpace == null) { //No available rooms to use
				Set<Vector3i> openSpace = SpaceFillingAlgorithm.findAvailableSpace(grid, being.location.coords, 
						requiredSpace.x * 3, requiredSpace.y * 3, true);
				int height = openSpace.iterator().next().z;
				
				int[] maxSubRect = AlgUtil.findMaxRect(openSpace);
				int bestR = maxSubRect[0], bestC = maxSubRect[1],
						rectR = maxSubRect[2], rectC = maxSubRect[3];
				
				Set<Integer> bestBuildingMaterials = society.getBestBuildingMaterials(calcUtility, 
						being, (requiredSpace.x + requiredSpace.y) * 2);
					
				List<Vector3i> bestRectangle = Vector3i.getRange(
						new Vector3i(bestR, bestC, height), new Vector3i(bestR + rectR - 1, bestC + rectC - 1, height));
				Collections.sort(bestRectangle, new Comparator<Vector3i>() {
					@Override
					public int compare(Vector3i o1, Vector3i o2) {
						return o2.manhattanDist(being.location.coords) - o1.manhattanDist(being.location.coords);
					}
				});
				
				if (openSpace != null) {
					priority = new ConstructRoomPriority(bestRectangle, bestBuildingMaterials);
				}
				else {
					priority = new ImpossiblePriority();
				}
			}
			else {
				priority = new BuildingPlacePriority(nearOpenSpace, ItemData.createItem(buildingId, 1));
			}
			return priority;
		}
		
		if (step.stepType.equals("I")) {
			if (being.processBuilding != null && being.processBuilding.inventory.hasItems(process.inputItems)) {
				being.processBuilding.inventory.subtractItems(process.inputItems);
				return new DonePriority(null);
			}
			else if (being.inventory.hasItems(process.inputItems)) {
				priority = new ItemDeliveryPriority(primaryLocation, new Inventory(process.inputItems));
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
					return new DonePriority(null);
				}
			}
			else {
				priority = new MovePriority(primaryLocation);
			}
		}
		else if (step.stepType.startsWith("U")) {
			step.timeTicks--;
			if (step.timeTicks <= 0) {
				return new DonePriority(null);
			}
		}
		else if (step.stepType.equals("O")) {
			List<InventoryItem> outputItems = process.outputItems.getOneItemDrop();
			destinationInventory.addItems(outputItems);
			if (!itemMode.equals("Personal")) { //Keep a record of this item in the 3d space
				for (InventoryItem item: outputItems) {
					grid.addItemRecordToWorld(primaryLocation, item);
				}
			}
			return new DonePriority(null);
		}
		return priority;
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
	}
	
	private static void assignAllHumanJobs(Society society) {
		List<Human> humans = society.getAllPeople();
		for (Human human: humans) {
			assignSingleHumanJob(society, human);
		}
	}
	private static void assignSingleHumanJob(Society society, Human human) {
		Map<Integer, Double> calcUtility = society.findCompleteUtilityAllItems(human);
		Map<Process, Double> bestProcesses = society.prioritizeProcesses(calcUtility, human, 20, null);
		Process randBiasedChosenProcess = MathUti.randChoiceFromWeightMap(bestProcesses);
		human.processProgress = randBiasedChosenProcess;
	}
	
	private static void collectivelyAssignJobsSociety(Society society) {
		Map<Integer, Double> calcUtility = society.findCompleteUtilityAllItems(null);
		Map<Process, Double> bestProcesses = society.prioritizeProcesses(calcUtility, null, 20, null);
		List<Human> humans = society.getAllPeople();
		
		int humanIndex = 0;
		for (Entry<Process, Double> entry: bestProcesses.entrySet()) {
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
	
	private static Priority progressToFindItem(LocalGrid grid, Vector3i centerCoords, int firstItemNeeded, int amountNeeded) {
		KdTree<Vector3i> nearestItemsTree = grid.getKdTreeForItemId(firstItemNeeded);
		
		if (nearestItemsTree == null) {
			System.out.println(ItemData.getNameFromId(firstItemNeeded));
			
			//Process harvestNeededProcess = ProcessData.getProcessByName("Harvest " + );
			return new ImpossiblePriority();
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
	
	/*
	private static Set<InventoryItem> lookForOwnedItems(Human human, Set<Integer> itemIds) {
		
	}
	
	private static Set<InventoryItem> findItemIdsToUse(Human human, Set<Integer> itemIds) {
		
	}
	*/
	
}
