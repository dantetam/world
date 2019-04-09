package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.github.dantetam.toolbox.MathUti;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.ai.Pathfinder;
import io.github.dantetam.world.ai.Pathfinder.ScoredPath;
import io.github.dantetam.world.civilization.Human;
import io.github.dantetam.world.civilization.LivingEntity;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.Process;
import io.github.dantetam.world.process.Process.ProcessStep;
import io.github.dantetam.world.process.priority.BuildingPlacePriority;
import io.github.dantetam.world.process.priority.DonePriority;
import io.github.dantetam.world.process.priority.ItemDeliveryBuildingPriority;
import io.github.dantetam.world.process.priority.ItemDeliveryPriority;
import io.github.dantetam.world.process.priority.ItemPickupBuildingPriority;
import io.github.dantetam.world.process.priority.ItemPickupPriority;
import io.github.dantetam.world.process.priority.MovePriority;
import io.github.dantetam.world.process.priority.Priority;
import io.github.dantetam.world.process.prioritytask.DropoffInvTask;
import io.github.dantetam.world.process.prioritytask.MoveTask;
import io.github.dantetam.world.process.prioritytask.PickupTask;
import io.github.dantetam.world.process.prioritytask.Task;
import kdtreegeo.KdTree;

public class LocalGridTimeExecution {
	
	public static double numDayTicks = 0;
	
	public static void tick(LocalGrid grid, Society society) {
		if (numDayTicks % 1440 == 0) {
			numDayTicks = 0;
			assignHumanJobsSingly(society);
		}
		for (Human human: society.getAllPeople()) {
			if (human.processProgress != null) {
				if (human.processProgress.requiredBuildNameOrGroup != null && human.processBuilding == null) {
					assignBuilding(grid, human, human.processProgress.requiredBuildNameOrGroup);
				}
				else if (human.processProgress.requiredTileNameOrGroup != null) {
					assignTile(grid, human, human.processProgress.requiredTileNameOrGroup);
				}
				
				if (human.processProgress.processSteps.size() == 0) {
					System.out.println("################");
					System.out.println(human.name + " completed process: " + human.processProgress);
					human.processProgress = null;
					
					if (human.processBuilding != null) {
						human.processBuilding.currentUser = null;
						human.processBuilding = null;
					}
					if (human.processTile != null) {
						human.processTile.harvestInUse = false;
						human.processTile = null;
					}
				}
				else {
					ProcessStep step = human.processProgress.processSteps.get(0);
					human.activePriority = getPriorityForStep(society, grid, human, human.processProgress, step);
					
					String priorityName = human.activePriority == null ? "null" : human.activePriority.getClass().getName();
					System.out.println("################");
					System.out.println(human.name + ", for its process: " + human.processProgress + ", ");
							System.out.println("was given the priority: " + priorityName);
					if (human.activePriority instanceof DonePriority) {
						human.processProgress.processSteps.remove(0);
						human.processProgress = null;
						human.activePriority = null;
					}
				}
			}
			if (human.activePriority != null) {
				human.currentQueueTasks = getTasksFromPriority(grid, human, human.activePriority);
				if (human.currentQueueTasks.size() > 0)
					System.out.println(human.currentQueueTasks.get(0).getClass());
			}
			if (human.currentQueueTasks != null && human.currentQueueTasks.size() > 0) {
				Task task = human.currentQueueTasks.get(0);
				if (task.taskTime == 0) {
					human.currentQueueTasks.remove(0);
					executeTask(grid, human, task);
				}
				else {
					task.taskTime--;
				}
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
				return tasks;
			}
			else {
				return getTasksFromPriority(grid, being, new MovePriority(itemPriority.coords));
			}
		}
		else if (priority instanceof ItemDeliveryPriority) {
			ItemDeliveryPriority itemPriority = (ItemDeliveryPriority) priority;
			if (itemPriority.coords.manhattanDist(being.location.coords) <= 1) {
				being.inventory.subtractItems(itemPriority.inventory.getItems());
				grid.getTile(itemPriority.coords).itemsOnFloor.addItems(itemPriority.inventory.getItems());
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
				return tasks;
			}
			else {
				return getTasksFromPriority(grid, being, new MovePriority(itemPriority.coords));
			}
		}
		else if (priority instanceof ItemDeliveryBuildingPriority) {
			ItemDeliveryBuildingPriority itemPriority = (ItemDeliveryBuildingPriority) priority;
			if (itemPriority.coords.manhattanDist(being.location.coords) <= 1) {
				being.inventory.subtractItems(itemPriority.inventory.getItems());
				grid.getTile(itemPriority.coords).building.inventory.addItems(itemPriority.inventory.getItems());
				return tasks;
			}
			else {
				return getTasksFromPriority(grid, being, new MovePriority(itemPriority.coords));
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
	
	private static Priority getPriorityForStep(Society society, LocalGrid grid, LivingEntity being, Process process, ProcessStep step) {
		Priority priority = null;
		
		System.out.println("Figuring out for process: " + process);
		
		Vector3i primaryLocation = null;
		if (being.processBuilding != null) {
			primaryLocation = being.processBuilding.getPrimaryLocation();
		}
		else if (being.processTile != null) {
			primaryLocation = being.processTile.coords;
		}
		else if (being.processProgress.requiredBuildNameOrGroup == null) {
			primaryLocation = being.location.coords;
		}
		
		if (primaryLocation == null) {
			int buildingId = ItemData.getIdFromName(being.processProgress.requiredBuildNameOrGroup);
			Vector3i nearOpenSpace = grid.getNearOpenSpace(society.societyCenter);
			priority = new BuildingPlacePriority(nearOpenSpace, ItemData.createItem(buildingId, 1));
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
				
				System.out.println(regularItemNeeds);
				
				int firstItemNeeded = (Integer) regularItemNeeds.keySet().toArray()[0];
				int amountNeeded = regularItemNeeds.get(firstItemNeeded);
				KdTree<Vector3i> nearestItemsTree = grid.getKdTreeForItemId(firstItemNeeded);
				
				if (nearestItemsTree == null) {
					System.out.println(ItemData.getNameFromId(firstItemNeeded));
				}
				
				int numCandidates = Math.min(nearestItemsTree.size(), 10);
				Collection<Vector3i> nearestCoords = nearestItemsTree.nearestNeighbourListSearch(
						numCandidates, being.location.coords);
			
				Map<Vector3i, Double> score = new HashMap<>();
				for (Vector3i itemCoords: nearestCoords) {
					int numItemsAtTile = grid.getTile(itemCoords).itemsOnFloor.findItemCount(firstItemNeeded);
					numItemsAtTile += grid.getTile(itemCoords).building.inventory.findItemCount(firstItemNeeded);
					int distUtil = being.location.coords.manhattanDist(itemCoords);
					score.put(itemCoords, Math.min(numItemsAtTile, amountNeeded) / Math.pow(distUtil, 2));
				}
				score = MathUti.getSortedMapByValueDesc(score);
				
				Vector3i location = (Vector3i) score.keySet().toArray()[0];
				InventoryItem itemClone = new InventoryItem(firstItemNeeded, amountNeeded, ItemData.getNameFromId(firstItemNeeded));
				priority = new ItemPickupPriority(location, itemClone);
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
			being.processBuilding.inventory.addItems(process.outputItems.getOneItemDrop());
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
	
	private static void assignHumanJobsSingly(Society society) {
		List<Human> humans = society.getAllPeople();
		for (Human human: humans) {
			Map<Integer, Double> calcUtility = society.findCompleteUtilityAllItems(human);
			Map<Process, Double> bestProcesses = society.prioritizeProcesses(calcUtility, human, 10);
			Process bestProcess = (Process) bestProcesses.keySet().toArray()[0];
			human.processProgress = bestProcess;
		}
	}
	
	private static void assignAllHumansJobs(Society society) {
		Map<Integer, Double> calcUtility = society.findCompleteUtilityAllItems(null);
		Map<Process, Double> bestProcesses = society.prioritizeProcesses(calcUtility, null, 20);
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
	
}
