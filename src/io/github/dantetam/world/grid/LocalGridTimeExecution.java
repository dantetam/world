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
import java.util.function.Function;

import io.github.dantetam.toolbox.VecGridUtil;
import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.Pair;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.ai.Pathfinder;
import io.github.dantetam.world.ai.Pathfinder.ScoredPath;
import io.github.dantetam.world.civhumansocietyai.EmergentSocietyCalc;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ProcessData;
import io.github.dantetam.world.grid.ItemMetricsUtil.ItemMetric;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.process.LocalJob;
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
import io.github.dantetam.world.process.priority.MoveTolDistOnePriority;
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
		
		Date date = world.getTime();
		
		System.out.println("<<<<>>>> Date: " + date);
		if (date.getHours() == 0) { //Every day, assign new jobs
			society.createJobOffers();
			assignAllHumanJobs(society, date);
			society.leadershipManager.workThroughSocietalPolitics();
		}
		
		System.out.println("################");
		for (Human human: society.getAllPeople()) {
			String processName = human.processProgress == null ? "null" : human.processProgress.name;
			System.out.println(human.name + " located at " + human.location.coords + 
					", with process: " + processName); 
			System.out.println("Inventory (counts): " + human.inventory.toUniqueItemsMap());
					// + human.processProgress == null ? "null" : human.processProgress.name);
			System.out.println("Wealth: " + human.getTotalWealth() + 
					", buildings: " + human.ownedBuildings.size() +
					", num items: " + human.ownedItems.size());
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
				
				if (human.currentQueueTasks != null) {
					String firstTaskString = human.currentQueueTasks.size() > 0 ? human.currentQueueTasks.get(0).toString() : "tasks is empty";
					System.out.println(human.name + " was assigned " + human.currentQueueTasks.size() + " tasks, first: " + firstTaskString); 
				}
				else {
					System.out.println(human.name + " was assigned NULL tasks"); 
				}
				
				if (human.currentQueueTasks != null && human.currentQueueTasks.size() > 0)
					System.out.println(human.currentQueueTasks.get(0).getClass());
				if (human.currentQueueTasks == null) {
					human.activePriority = null;
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
						human.activePriority = getPriorityForStep(society, grid, 
								human, human.jobProcessProgress.boss, process, step);
						String priorityName = human.activePriority == null ? "null" : human.activePriority.getClass().getSimpleName();
						System.out.println(human.name + ", for its process: " + process + " (first step), ");
								System.out.println("was given the PRIORITY: " + priorityName);
					}
					if (human.activePriority instanceof DonePriority || 
							human.activePriority instanceof ImpossiblePriority) {
						if (human.activePriority instanceof ImpossiblePriority) {
							ImpossiblePriority iPrior = (ImpossiblePriority) human.activePriority;
							System.err.println("Warning, impossible priority: " + iPrior.reason);
						}
						process.processSteps.remove(0);
						human.activePriority = null;
					}
					if (human.activePriority instanceof WaitPriority) {
						human.activePriority = null; //Spend a frame and advance priority/step/etc.
					}
				}
				//Process has been completed, remove it from person
				if (process.processSteps.size() == 0) { 
					executeProcessResActions(grid, human, process.processResActions);
					System.err.println(human.name + " COMPLETED process (as an employee for a job): " + process);
					human.jobProcessProgress = null; //Reset all jobs and job fields to null
					if (human.processBuilding != null) {
						human.processBuilding.currentUser = null;
						human.processBuilding = null;
					}
					if (human.processTile != null || human.targetTile != null) {
						human.processTile.harvestInUse = false;
						human.processTile = null;
						human.targetTile = null;
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
						human.activePriority = getPriorityForStep(society, grid, human, human, human.processProgress, step);
						String priorityName = human.activePriority == null ? "null" : human.activePriority.getClass().getSimpleName();
						System.out.println(human.name + ", for its process: " + human.processProgress + ", ");
								System.out.println("was given the PRIORITY: " + priorityName);
					}
					if (human.activePriority instanceof DonePriority || 
							human.activePriority instanceof ImpossiblePriority) {
						/*
						if (human.activePriority instanceof ImpossiblePriority) {
							System.err.println("Warning, impossible priority task was given, for process step: " + human.processProgress.processSteps.get(0));
						}
						*/
						human.processProgress.processSteps.remove(0);
						human.activePriority = null;
					}
					if (human.activePriority instanceof WaitPriority) {
						human.activePriority = null; //Spend a frame and advance priority/step/etc.
					}
				}
				//Process has been completed, remove it from person
				if (human.processProgress.processSteps.size() == 0) { 
					System.err.println(human.name + " COMPLETED process: " + human.processProgress);
					executeProcessResActions(grid, human, human.processProgress.processResActions);
					human.processProgress = null; //Reset all jobs and job fields to null
					if (human.processBuilding != null) {
						human.processBuilding.currentUser = null;
						human.processBuilding = null;
					}
					if (human.processTile != null || human.targetTile != null) {
						human.processTile.harvestInUse = false;
						human.processTile = null;
						human.targetTile = null;
					}
					assignSingleHumanJob(society, human, date);
				}
			}
			
			//Assign a new process. Note that a human may have a priority not linked to any higher structure,
			//which we do not want to override or chain incorrectly to a new job.
			if (human.processProgress == null && human.activePriority == null) {
				assignSingleHumanJob(society, human, date);
			}
			System.out.println();
		}
	}
	
	private static LocalBuilding assignBuilding(LocalGrid grid, LivingEntity being, 
			List<LivingEntity> validOwners, String buildingName) {
		int id = ItemData.getIdFromName(buildingName);
		KdTree<Vector3i> nearestItemsTree = grid.getKdTreeForBuildings(id);
		if (nearestItemsTree == null || nearestItemsTree.size() == 0) return null;
		int numCandidates = Math.min(nearestItemsTree.size(), 10);
		Collection<Vector3i> nearestCoords = nearestItemsTree.nearestNeighbourListSearch(
				numCandidates, being.location.coords);
		for (Vector3i nearestCoord: nearestCoords) {
			LocalBuilding building = grid.getTile(nearestCoord).building;
			System.out.println(building);
			if (building.owner == null || validOwners.contains(building.owner)) {
				if (building.currentUser == null) {
					
					Set<Vector3i> neighbors = grid.getAllNeighbors14(nearestCoord);
					neighbors.add(nearestCoord);
					for (Vector3i neighbor: neighbors) {
						ScoredPath scoredPath = grid.pathfinder.findPath(
								being, being.location, grid.getTile(neighbor));
						if (scoredPath.isValid()) {
							building.currentUser = being;
							being.processBuilding = building;
						}
					}
					
				}
			}
		}
		return being.processBuilding;
	}
	
	/**
	 * @param grid
	 * @param being
	 * @param validOwners
	 * @param tileName
	 * @return The harvest tile, followed by the tile to move to (within <= 1 distance)
	 */
	private static Pair<LocalTile> assignTile(LocalGrid grid, LivingEntity being, 
			List<LivingEntity> validOwners, String tileName) {
		int tileId = ItemData.getIdFromName(tileName);
		KdTree<Vector3i> items = grid.getKdTreeForTile(tileId);
		if (items == null) return null;
		Collection<Vector3i> candidates = items.nearestNeighbourListSearch(10, being.location.coords);
		Map<Pair<LocalTile>, Double> tileByPathScore = new HashMap<>();
		for (Vector3i candidate: candidates) {
			System.out.println("Calc path: " + being.location.coords + " to -> " + grid.getTile(candidate).coords);
			LocalTile tile = grid.getTile(candidate);
			
			if (tile.humanClaim == null || validOwners.contains(tile.humanClaim)) {
				if (!tile.harvestInUse) {
					Set<Vector3i> neighbors = grid.getAllNeighbors14(candidate);
					neighbors.add(candidate);
					for (Vector3i neighbor: neighbors) {
						ScoredPath scoredPath = grid.pathfinder.findPath(
								being, being.location, grid.getTile(neighbor));
						if (scoredPath.isValid()) {
							System.out.println(scoredPath.path);
							Pair<LocalTile> pair = new Pair(grid.getTile(candidate), grid.getTile(neighbor));
							tileByPathScore.put(pair, scoredPath.score);
						}
					}
				}
			}
		}
		tileByPathScore = MapUtil.getSortedMapByValueDesc(tileByPathScore);
		for (Object obj: tileByPathScore.keySet().toArray()) {
			Pair<LocalTile> bestPair = (Pair) obj;
			being.processTile = bestPair.second;
			being.targetTile = bestPair.first;
			bestPair.first.harvestInUse = true;
			return bestPair;
		}
		return null;
	}
	
	/**
	 * Deconstruct a process into a priority.
	 * @param being 		 The person that is completing this job or process, who is doing the physical activity
	 * @param ownerProducts  The person to which the final goods should go to (for employees working under a boss)
	 * @return the Priority object which represents the given group of actions/approach, for working towards
	 * 		the given process and step.
	 */
	private static Priority getPriorityForStep(Society society, LocalGrid grid, 
			Human being, Human ownerProducts, LocalProcess process, ProcessStep step) {
		Priority priority = null;
		
		//System.out.println("Figuring out priority, for process: " + process);
		
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
				priority = new ImpossiblePriority("Could not find open rectangular space");
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
		else if (process.requiredBuildNameOrGroup == null && process.requiredTileNameOrGroup == null) {
			primaryLocation = being.location.coords;
			destinationInventory = being.inventory;
			itemMode = "Personal";
		}
		else {
			System.err.println("Warning, building and/or tile required: " + 
					process.toString() + ", tile/building may not be assigned");
			return new ImpossiblePriority("Warning, could not find case for process. Tile/building may not be assigned");
		}
		
		Vector3i targetLocation = primaryLocation;
		if (being.targetTile != null)
			targetLocation = being.targetTile.coords;
		
		//For the case where a person has not been assigned a building/space yet
		if ((primaryLocation == null && process.requiredBuildNameOrGroup != null)
				|| process.isCreatedAtSite) {
			System.out.println("Find status building case");
			System.out.println(process.requiredBuildNameOrGroup);
			System.out.println(process);
			
			int buildingId; 
			Vector2i requiredSpace;
			
			if (process.requiredBuildNameOrGroup != null) {
				buildingId = ItemData.getIdFromName(process.requiredBuildNameOrGroup);
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
					priority = new ImpossiblePriority("Could not create building space of size: " + requiredSpace);
				}
				return priority;
			}
			else {
				Vector3i nearOpenSpace = (Vector3i) boundsData[0]; // = grid.getNearOpenSpace(society.societyCenter);
				Vector2i bounds2d = (Vector2i) boundsData[1];
				
				System.out.println("Assigned building space: " + nearOpenSpace.toString());
				grid.setInUseRoomSpace(nearOpenSpace, bounds2d, true);
				
				if (process.isCreatedAtSite) {
					being.processTile = grid.getTile(nearOpenSpace);
					if (being.location.coords.areAdjacent14(targetLocation)) {
						//continue
					}
					else {
						return new MovePriority(primaryLocation);
					}
				}
				else { //implies process.requiredBuildNameOrGroup != null -> buildingId is valid
					priority = new BuildingPlacePriority(nearOpenSpace, 
							ItemData.createItem(buildingId, 1), ownerProducts);
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
				if (being.location.coords.areAdjacent14(primaryLocation)) {
					return new DonePriority();
				}
				else {
					priority = new MoveTolDistOnePriority(primaryLocation);
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
			if (being.location.coords.areAdjacent14(targetLocation)) {
				step.timeTicks--;
				if (step.timeTicks <= 0) {
					return new DonePriority();
				}
				else {
					priority = new WaitPriority();
				}
			}
			else {
				priority = new MoveTolDistOnePriority(targetLocation);
			}
		}
		else if (step.stepType.equals("HBuilding")) {
			LocalTile tile = grid.getTile(targetLocation);
			if (tile.building == null) {
				return new DonePriority();
			}
			if (being.location.coords.areAdjacent14(targetLocation)) {
				priority = new BuildingHarvestPriority(tile.coords, tile.building);
			}
			else {
				priority = new MoveTolDistOnePriority(targetLocation);
			}
		}
		else if (step.stepType.equals("HTile")) {
			LocalTile tile = grid.getTile(targetLocation);
			if (tile.tileBlockId == ItemData.ITEM_EMPTY_ID || step.timeTicks <= 0) {
				return new DonePriority();
			}
			if (being.location.coords.areAdjacent14(targetLocation)) {
				priority = new TileHarvestPriority(tile.coords);
			}
			else {
				priority = new MoveTolDistOnePriority(targetLocation);
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
			for (InventoryItem item: outputItems) {
				item.owner = ownerProducts;
			}
			System.out.println("Dropped items: " + new Inventory(outputItems) + " at place: " + itemMode);
			if (being.inventory.canFitItems(outputItems) && (itemMode.equals("Personal") || itemMode.equals("Tile"))) {
				being.inventory.addItems(outputItems);
			}
			else {
				destinationInventory.addItems(outputItems);
				//Keep a record of this item in the 3d space (inventory has no scope/access to world item records)
				for (InventoryItem item: outputItems) {
					grid.addItemRecordToWorld(primaryLocation, item);
				}
			}
			return new DonePriority();
		}
		else if (step.stepType.equals("B")) {
			List<InventoryItem> outputItems = process.outputItems.getOneItemDrop();
			if (outputItems.size() == 0) {
				return new DonePriority();
			}
			InventoryItem buildingItem = outputItems.get(0);
			if (!ItemData.isBuildingId(buildingItem.itemId)) {
				throw new IllegalArgumentException("Could not create building in process step, "
						+ "given non-building id: " + buildingItem.itemId + ", process: " + process);
			}
			
			LocalBuilding building = ItemData.building(buildingItem.itemId);
			
			System.out.println("Built building: " + building.name + " at place: " + itemMode);
			grid.addBuilding(building, targetLocation, true, ownerProducts);
			return new DonePriority();
		}
		
		if (!step.stepType.startsWith("U") && priority == null)
			System.err.println("Warning, case not covered for priority from step, returning null priority");
		return priority;
	}
	
	/**
	 * The last step of process deconstruction, where priorities are given tasks, according to world states.
	 * 
	 * Use TaskListPlaceholders to have less ambiguous priority end conditions and signals.
	 * 
	 * @param grid
	 * @param being
	 * @param priority
	 * @return The list of tasks needed to be done to progress in the priority;
	 * 			otherwise, null, if the priority has been completed
	 */
	private static List<Task> getTasksFromPriority(LocalGrid grid, LivingEntity being, Priority priority) {
		List<Task> tasks = new ArrayList<>();
		if (priority instanceof ItemPickupPriority) {
			ItemPickupPriority itemPriority = (ItemPickupPriority) priority;
			if (itemPriority.coords.areAdjacent14(being.location.coords)) {
				grid.getTile(itemPriority.coords).itemsOnFloor.subtractItem(itemPriority.item);
				being.inventory.addItem(itemPriority.item);
				grid.removeItemRecordToWorld(itemPriority.coords, itemPriority.item);
				return null; TODO //Implement/see ImpossibleTaskPlaceholder/DoneTaskPlaceholder;
			}
			else {
				return getTasksFromPriority(grid, being, new MoveTolDistOnePriority(itemPriority.coords));
			}
		}
		else if (priority instanceof ItemDeliveryPriority) {
			ItemDeliveryPriority itemPriority = (ItemDeliveryPriority) priority;
			if (itemPriority.coords.areAdjacent14(being.location.coords)) {
				List<InventoryItem> desiredItems = itemPriority.inventory.getItems();
				being.inventory.subtractItems(desiredItems);
				grid.getTile(itemPriority.coords).itemsOnFloor.addItems(desiredItems);
				return null;
			}
			else {
				return getTasksFromPriority(grid, being, new MoveTolDistOnePriority(itemPriority.coords));
			}
		}
		else if (priority instanceof ItemPickupBuildingPriority) {
			ItemPickupBuildingPriority itemPriority = (ItemPickupBuildingPriority) priority;
			if (itemPriority.coords.areAdjacent14(being.location.coords)) {
				grid.getTile(itemPriority.coords).building.inventory.subtractItem(itemPriority.item);
				being.inventory.addItem(itemPriority.item);
				
				grid.removeItemRecordToWorld(itemPriority.coords, itemPriority.item);
				return null;
			}
			else {
				return getTasksFromPriority(grid, being, new MoveTolDistOnePriority(itemPriority.coords));
			}
		}
		else if (priority instanceof ItemDeliveryBuildingPriority) {
			ItemDeliveryBuildingPriority itemPriority = (ItemDeliveryBuildingPriority) priority;
			if (itemPriority.coords.areAdjacent14(being.location.coords)) {
				List<InventoryItem> desiredItems = itemPriority.inventory.getItems();
				being.inventory.subtractItems(desiredItems);
				grid.getTile(itemPriority.coords).building.inventory.addItems(desiredItems);
				
				grid.addItemRecordsToWorld(itemPriority.coords, desiredItems);
				return null;
			}
			else {
				return getTasksFromPriority(grid, being, new MoveTolDistOnePriority(itemPriority.coords));
			}
		}
		else if (priority instanceof TileHarvestPriority) {
			TileHarvestPriority tilePriority = (TileHarvestPriority) priority;
			LocalTile hTile = grid.getTile(tilePriority.coords);
			if (hTile == null || hTile.tileBlockId == ItemData.ITEM_EMPTY_ID) {
				return null;
			}
			if (tilePriority.coords.areAdjacent14(being.location.coords)) {
				int pickupTime = ItemData.getPickupTime(hTile.tileBlockId);
				tasks.add(new HarvestBlockTileTask(pickupTime, tilePriority.coords));
			}
			else {
				return getTasksFromPriority(grid, being, new MoveTolDistOnePriority(tilePriority.coords));
			}
		}
		else if (priority instanceof BuildingPlacePriority) {
			BuildingPlacePriority buildPriority = (BuildingPlacePriority) priority;
			
			if (buildPriority.coords.areAdjacent14(being.location.coords)) {
				LocalTile tile = grid.getTile(buildPriority.coords);
				if (being.inventory.hasItem(buildPriority.buildingItem) &&
						tile.building == null) {
					being.inventory.subtractItem(buildPriority.buildingItem);
					LocalBuilding newBuilding = ItemData.building(buildPriority.buildingItem.itemId);
					grid.addBuilding(newBuilding, buildPriority.coords, false, buildPriority.owner);
					return null;
				}
			}
			else {
				return getTasksFromPriority(grid, being, new MoveTolDistOnePriority(buildPriority.coords));
			}
		}
		else if (priority instanceof BuildingHarvestPriority) {
			BuildingHarvestPriority buildPriority = (BuildingHarvestPriority) priority;
			LocalTile tile = grid.getTile(buildPriority.coords);
			
			if (tile == null || tile.building == null) {
				return null;
			}
			if (buildPriority.coords.areAdjacent14(being.location.coords)) {	
				int pickupTime = ItemData.getPickupTime(tile.tileBlockId);
				tasks.add(new HarvestBuildingTask(pickupTime, buildPriority.coords, tile.building));
			}
			else {
				return getTasksFromPriority(grid, being, new MoveTolDistOnePriority(buildPriority.coords));
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
				if (consPriority.remainingBuildCoords.size() == 0) {
					return null;
				}
				bestLocation = consPriority.remainingBuildCoords.remove(0);
			}
			
			Collection<Integer> rankedMaterials = consPriority.rankedBuildMaterials;
			for (int materialId: rankedMaterials) {
				if (being.inventory.findItemCount(materialId) > 0) {
					return getTasksFromPriority(grid, being, new TilePlacePriority(bestLocation, materialId));
				}
			}
			
			for (int materialId: rankedMaterials) {
				Priority itemSearchPriority = progressToFindItem(grid, being.location.coords, materialId, 1);
				if (!(itemSearchPriority instanceof ImpossiblePriority)) {
					return getTasksFromPriority(
							grid, being,
							itemSearchPriority
							);
				}
			}
			return null;
		}
		else if (priority instanceof MovePriority) {
			MovePriority movePriority = (MovePriority) priority;
			
			if (movePriority.coords.equals(being.location.coords)) { 
				return null;
			}
			
			ScoredPath scoredPath = grid.pathfinder.findPath(
					being, being.location, grid.getTile(movePriority.coords));
			if (scoredPath.isValid()) {
				List<LocalTile> path = scoredPath.path;
				for (int i = 0; i < path.size() - 1; i++) {
					Vector3i coordsFrom = path.get(i).coords;
					Vector3i coordsTo = path.get(i+1).coords;
					tasks.add(new MoveTask(1, coordsFrom, coordsTo));
				}
			}
			else {
				System.err.println("Warning, path was not valid from " + being.location + ", " + grid.getTile(movePriority.coords));
				return null;
			}
		}
		else if (priority instanceof MoveTolDistOnePriority) {
			MoveTolDistOnePriority movePriority = (MoveTolDistOnePriority) priority;
			
			if (movePriority.coords.areAdjacent14(being.location.coords)) { 
				return null;
			}
			
			//Allow being to move to the actual space or any tile one distance unit away
			Set<Vector3i> validSpace = grid.getAllNeighbors14(movePriority.coords);
			validSpace.add(movePriority.coords);
			
			for (Vector3i candidateDest: validSpace) {
				ScoredPath scoredPath = grid.pathfinder.findPath(
						being, being.location.coords, candidateDest);
				if (scoredPath.isValid()) {
					List<LocalTile> path = scoredPath.path;
					for (int i = 0; i < path.size() - 1; i++) {
						Vector3i coordsFrom = path.get(i).coords;
						Vector3i coordsTo = path.get(i+1).coords;
						tasks.add(new MoveTask(1, coordsFrom, coordsTo));
					}
					return tasks;
				}
			}
			
			return null;
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
				return null;
			}
			else {
				for (int randIndex = 0; randIndex < locations.size(); randIndex++) {
					Vector3i randomLocation = locations.get(randIndex);
					if (randomLocation.areAdjacent14(being.location.coords)) {
						//return null;
					}
					else {
						return getTasksFromPriority(grid, being, new MoveTolDistOnePriority(randomLocation));
					}
				}
				return tasks;
			}
		}
		else if (priority instanceof DonePriority) {
			return null;
		}
		return tasks;
	}
	
	private static void executeProcessResActions(LocalGrid grid, LivingEntity being, List<ProcessStep> actions) {
		if (actions != null) {
			for (ProcessStep action: actions) {
				executeProcessResAction(grid, being, action);
			}
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
			TODO
		}
	}
	
	private static void executeTask(LocalGrid grid, LivingEntity being, Task task) {
		if (task instanceof MoveTask) {
			MoveTask moveTask = (MoveTask) task;
			grid.movePerson(being, grid.getTile(moveTask.coordsDest));
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
	
	private static void assignAllHumanJobs(Society society, Date date) {
		List<Human> humans = society.getAllPeople();
		for (Human human: humans) {
			assignSingleHumanJob(society, human, date);
		}
	}
	private static void assignSingleHumanJob(Society society, Human human, Date date) {
		Map<Integer, Double> calcUtility = society.findCompleteUtilityAllItems(human);
		
		Map<LocalProcess, Double> bestProcesses = society.prioritizeProcesses(calcUtility, human, 20, null);
		Map<LocalJob, Double> bestJobs = society.prioritizeJobs(human, bestProcesses, date);
		
		Object potentialJob = MapUtil.randChoiceFromMaps(bestProcesses, bestJobs);
		if (potentialJob instanceof LocalJob && human.jobProcessProgress == null) {
			human.jobProcessProgress = (LocalJob) potentialJob;
		}
		else if (potentialJob instanceof LocalProcess && human.processProgress == null) {
			human.processProgress = (LocalProcess) potentialJob;
		}
		//LocalProcess randBiasedChosenProcess = MapUtil.randChoiceFromWeightMap(bestProcesses);
		//human.processProgress = randBiasedChosenProcess;
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
				System.out.println(process);
				if (process.name.startsWith("Harvest ")) {
					int inputTileId = ItemData.getIdFromName(process.requiredTileNameOrGroup);
					double pickupTime = ItemData.getPickupTime(inputTileId);
					double expectedNumItem = process.outputItems.itemExpectation().get(inputTileId);
					
					KdTree<Vector3i> itemTree = grid.getKdTreeForTile(inputTileId);
					if (itemTree == null || itemTree.size() == 0) continue;
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
				return new ImpossiblePriority("Could not find items or processes that can output harvest");
			}
		}
		
		int numCandidates = Math.min(nearestItemsTree.size(), 10);
		Collection<Vector3i> nearestCoords = nearestItemsTree.nearestNeighbourListSearch(
				numCandidates, centerCoords);
	
		Map<Vector3i, Double> score = new HashMap<>();
		for (Vector3i itemCoords: nearestCoords) {
			LocalTile tile = grid.getTile(itemCoords);
			int numItemsAtTile = tile.itemsOnFloor.findItemCount(firstItemNeeded);
			if (tile.building != null) {
				numItemsAtTile += tile.building.inventory.findItemCount(firstItemNeeded);
			}
			int distUtil = centerCoords.manhattanDist(itemCoords);
			score.put(itemCoords, Math.min(numItemsAtTile, amountNeeded) / Math.pow(distUtil, 2));
		}
		score = MapUtil.getSortedMapByValueDesc(score);
		
		Vector3i location = (Vector3i) score.keySet().toArray()[0];
		InventoryItem itemClone = new InventoryItem(firstItemNeeded, amountNeeded, ItemData.getNameFromId(firstItemNeeded));
		
		return new ItemPickupPriority(location, itemClone);
	}
	
	private static Priority progressToFindItemGroup(LocalGrid grid, Vector3i centerCoords,
			String itemGroupName, int amountNeeded,
			ItemMetric scoringMetric) { 
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
				return new ImpossiblePriority("Could not find item groups or processes that can output harvest groups");
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
		score = MapUtil.getSortedMapByValueDesc(score);		
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
