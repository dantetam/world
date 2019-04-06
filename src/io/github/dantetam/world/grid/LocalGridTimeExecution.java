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
import io.github.dantetam.world.civilization.Human;
import io.github.dantetam.world.civilization.LivingEntity;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.Process;
import io.github.dantetam.world.process.Process.ProcessStep;
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
			assignAllHumansJobs(society);
		}
		for (Human human: society.getAllPeople()) {
			if (human.processProgress != null) {
				if (human.processProgress.requiredBuildNameOrGroup != null && human.processBuilding == null) {
					assignBuilding(grid, human, human.processProgress.requiredBuildNameOrGroup);
				}
				if (human.processProgress.processSteps.size() == 0) {
					human.processProgress = null;
				}
				else {
					ProcessStep step = human.processProgress.processSteps.get(0);
					human.activePriority = getPriorityForStep(grid, human, human.processProgress, step);
					if (human.activePriority instanceof DonePriority) {
						human.processProgress.processSteps.remove(0);
						human.processProgress = null;
						human.activePriority = null;
					}
				}
			}
			if (human.activePriority != null) {
				human.currentQueueTasks = getTasksFromPriority(grid, human, human.activePriority);
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
			}
		}
		return being.processBuilding;
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
		else if (priority instanceof MovePriority) {
			MovePriority movePriority = (MovePriority) priority;
			List<LocalTile> path = Pathfinder.findPath(grid, being, being.location, grid.getTile(movePriority.coords));
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
	
	private static Priority getPriorityForStep(LocalGrid grid, LivingEntity being, Process process, ProcessStep step) {
		Priority priority = null;
		if (step.stepType.equals("I")) {
			if (being.processBuilding.inventory.hasItems(process.inputItems)) {
				being.processBuilding.inventory.subtractItems(process.inputItems);
				return new DonePriority(null);
			}
			else if (being.inventory.hasItems(process.inputItems)) {
				Vector3i coords = being.processBuilding.calculatedLocations.get(0);
				priority = new ItemDeliveryPriority(coords, new Inventory(process.inputItems));
			}
			else {
				Object[] invData = being.inventory.findRemainingItemsNeeded(process.inputItems);
				Map<Integer, Integer> regularItemNeeds = (Map) invData[0];
				
				int firstItemNeeded = (Integer) regularItemNeeds.keySet().toArray()[0];
				int amountNeeded = regularItemNeeds.get(firstItemNeeded);
				KdTree<Vector3i> nearestItemsTree = grid.getKdTreeForItemId(firstItemNeeded);
				
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
			Vector3i primaryLocation = null;
			if (being.processProgress.requiredBuildNameOrGroup == null) {
				primaryLocation = being.location.coords;
			}
			else if (being.processBuilding != null) {
				primaryLocation = being.processBuilding.calculatedLocations.get(0);
			}
			if (primaryLocation == null) {
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
			else {
				//priority = new BuildingPlacePriority(primaryLocation);
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
	
	private static void assignAllHumansJobs(Society society) {
		Map<Integer, Double> calcUtility = society.findCompleteUtilityAllItems();
		Map<Process, Double> bestProcesses = society.prioritizeProcesses(calcUtility, 20);
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
