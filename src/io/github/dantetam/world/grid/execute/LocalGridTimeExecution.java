package io.github.dantetam.world.grid.execute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.function.Function;

import org.apache.commons.math3.distribution.NormalDistribution;

import io.github.dantetam.toolbox.VecGridUtil;
import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.toolbox.CollectionUtil;
import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.MathAndDistrUti;
import io.github.dantetam.toolbox.Pair;
import io.github.dantetam.toolbox.StringUtil;
import io.github.dantetam.vector.Vector2i;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.ai.Pathfinder.ScoredPath;
import io.github.dantetam.world.civhumanai.Ethos;
import io.github.dantetam.world.civhumanai.NeedsGamut;
import io.github.dantetam.world.civhumanrelation.HumanHumanRel;
import io.github.dantetam.world.civhumanrelation.HumanHumanRel.HumanHumanRelType;
import io.github.dantetam.world.civilization.LocalExperience;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.civilization.artwork.ArtworkGraph;
import io.github.dantetam.world.civilization.gridstructure.AnnotatedRoom;
import io.github.dantetam.world.civilization.gridstructure.PurposeAnnoBuildDesPriority;
import io.github.dantetam.world.civilization.gridstructure.PurposeAnnotatedBuild;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ProcessData;
import io.github.dantetam.world.grid.GridRectInterval;
import io.github.dantetam.world.grid.ItemMetricsUtil;
import io.github.dantetam.world.grid.LocalBuilding;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.grid.SpaceFillingAlg;
import io.github.dantetam.world.grid.WorldGrid;
import io.github.dantetam.world.grid.ItemMetricsUtil.*;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.items.InventoryItem.ItemQuality;
import io.github.dantetam.world.items.ItemProperty;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.process.LocalJob;
import io.github.dantetam.world.process.LocalProcess;
import io.github.dantetam.world.process.LocalProcess.ProcessStep;
import io.github.dantetam.world.process.LocalSocietyJob;
import io.github.dantetam.world.process.priority.*;
import io.github.dantetam.world.process.prioritytask.*;
import kdtreegeo.KdTree;

//TODO
//Remove grid specific processes for humans and allow humans to considering doing jobs
//transporting themselves, resources, etc. across grids. Also allow for people to travel between grids.

public class LocalGridTimeExecution {
	
	public static int NUM_JOBPROCESS_CONSIDER = 40;
	
	/** 
	 * Primarily tick through all of the grid's 'unsupervised' tasks, i.e. those
	 * 
	 * 1) assigned to the natural world;
	 * 2) assigned to a human as a task not needing human input/time in some intermediate stage
	 * 			(like waiting for food waste turn into compost)
	 * 
	 * The second case is denoted as "U..." in the process data csv, "world-recipes.csv"
	 * 
	 */
	
	public static void specificGridTick(WorldGrid world, LocalGrid grid) {
		//TODO;
		//Implement ticking of natural tasks
		//Fix unsupervised work bug by bringing tasks into the 'grid queue', 
		//and when human input is needed, shift the task back into the human's queue.
		
		for (LivingEntity being: grid.getAllLivingBeings()) {
			if (!(being instanceof Human)) {
				if (being.processProgress == null) {
					if (Math.random() < 0.5) {
						being.processProgress = ProcessData.getProcessByName("Eat Plants");
					}
					else {
						being.processProgress = ProcessData.getProcessByName("Wander Around");
					}
				}
				deconstructionProcessGeneric(being, grid, being.processProgress);
			}
		}
	}
	
	public static void tick(WorldGrid world, LocalGrid grid, Society society) {
		Date date = world.getTime();
		
		CustomLog.outPrintln("<<<<>>>> Date: " + date);
		
		//Every day, assign new jobs and new societal measures of utility
		if (date.getHours() == 0 || society.calcUtility == null) {
			society.createJobOffers();
			assignAllHumanJobs(society, grid, date);
			society.leadershipManager.workThroughSocietalPolitics();
			
			society.calcUtility = society.findCompleteUtilityAllItems(null);
			society.accessibleResUtil = society.findAllAvailableResourceRarity(null);
			society.groupItemRarity = society.findRawGroupsResRarity(null);
			society.importantLocations = society.getImportantLocations(society.societyCenter);
			society.adjEconomicUtility = society.findAdjEconomicRarity();
			
			society.humanNeedsMap = society.findAllNeedsIntensityMap();
			for (Entry<Human, NeedsGamut> entry: society.humanNeedsMap.entrySet()) {
				Human human = entry.getKey();
				NeedsGamut needs = entry.getValue();
				human.shortTermNeeds = needs;
			}
			society.totalNeedsGamut = society.finalTotalNeedsMap();
		}
		
		CustomLog.outPrintln("################");
		for (Human human: society.getAllPeople()) {
			boolean isJob = human.jobProcessProgress != null;
			boolean isPersonal = human.processProgress != null;
			if (isJob) {
				deconstructionJob(human, grid, society, date, human.jobProcessProgress);
			}
			else if (isPersonal) {
				deconstructionProcess(human, grid, society, date, human.processProgress);
			}
			else {
				assignSingleHumanJob(society, grid, human, date);
			}
			
			Iterator<LocalJob> uJobIter = human.unsupervisedJobs.iterator();
			while (uJobIter.hasNext()) {
				LocalJob job = uJobIter.next();
				boolean removeCond = deconstructionProcessGeneric(human, grid, job.jobWorkProcess);
				if (removeCond) {
					uJobIter.remove();
				}
			}
			
			Iterator<LocalProcess> uProcessIter = human.unsupervisedProcesses.iterator();
			while (uProcessIter.hasNext()) {
				LocalProcess process = uProcessIter.next();
				boolean removeCond = deconstructionProcessGeneric(human, grid, process);
				if (removeCond) {
					uProcessIter.remove();
				}
			}
		}
	}
	
	/**
		//While a process is in 'grid queue' i.e. it is unsupervised,
		//tick it down using the same LocalGridTimeExecution::getPriorityForStep(...) protocols,
		//but made generic with no humans involved.
		 * @return true if the process is done and must be removed from all queues
	 */
	public static boolean deconstructionProcessGeneric(LivingEntity unsupervHuman, LocalGrid grid, LocalProcess process) {
		//Follow through with one step, deconstruct into a priority, and get its status back.
		if (process.processStepIndex >= 0 && process.processStepIndex < process.processSteps.size()) {
			ProcessStep step = process.processSteps.get(process.processStepIndex);

			Priority activePriority = getPriorForStepUnsupervBasic(grid, unsupervHuman, process, step);
			
			if (activePriority instanceof DonePriority || 
					activePriority instanceof ImpossiblePriority) {
				if (activePriority instanceof ImpossiblePriority) {
					CustomLog.errPrintln("Warning, impossible for process step: " + process.processSteps.get(0));
					ImpossiblePriority iPrior = (ImpossiblePriority) activePriority;
					CustomLog.errPrintln("Warning, impossible priority: " + iPrior.reason);
				}
				return true;
			}
			if (activePriority instanceof WaitPriority) {
				activePriority = null; //Spend a frame and advance priority/step/etc.
			}
		}
		//Process has been completed, remove it from person
		if (process.processStepIndex < 0 || process.processStepIndex >= process.processSteps.size()) {
			process.recRepeats--;
			if (process.recRepeats <= 0) {
				return true;
			}
			else {
				process.processStepIndex = 0;
			}
		}
		
		return false;
	}
	
	public static void deconstructionProcess(Human human, LocalGrid grid, Society society, Date date, 
			LocalProcess process) {
		String processName = process == null ? "null" : process.name;
		CustomLog.outPrintln(human.name + " located at " + human.location.coords + 
				", with process: " + processName); 
		CustomLog.outPrintln("Inventory (counts): " + human.inventory.toUniqueItemsMap());
				// + human.processProgress == null ? "null" : human.processProgress.name);
		CustomLog.outPrintln("Wealth: " + human.getTotalWealth() + 
				", buildings: " + human.ownedBuildings.size() +
				", num items: " + human.ownedItems.size());
		CustomLog.outPrintln("Priority: " + human.activePriority);
		
		Set<Human> capitalOwners = new HashSet<Human>() {{
			add(human); 
			if (human.household != null) {
				addAll(human.household.householdMembers);
			}
		}};
		//Assign the location of the process where the steps are undertaken
		if (process.requiredBuildNameOrGroup != null && process.processBuilding == null) {
			assignBuilding(grid, human, process, capitalOwners, process.requiredBuildNameOrGroup);
		}
		else if (process.requiredTileNameOrGroup != null && process.processTile == null) {
			assignTile(grid, human, capitalOwners, process);
			System.err.println("_>>>>>>>>>>>>>>>>>>> + processTile (" + process.requiredTileNameOrGroup + 
					"): " + process.processTile + "\n");
		}
		
		if (human.currentQueueTasks != null && human.currentQueueTasks.size() > 0) {
			Task task = human.currentQueueTasks.get(0);
			CustomLog.outPrintln(human.name + " has " + human.currentQueueTasks.size() + " tasks."); 
			if (task.taskTime <= 0) {
				CustomLog.outPrintln(human.name + " FINISHED task: " + task.toString());
				human.currentQueueTasks.remove(0);
				executeTask(grid, human, task);
				if (human.currentQueueTasks.size() == 0) {
					human.activePriority = null;
				}
			}
			else {
				task.taskTime--;
				CustomLog.outPrintln(human.name + " task in progress: " 
						+ task.getClass().getSimpleName() + " (" + task.taskTime + ")");
			}
		}
		if (human.activePriority != null && //Assign tasks if there are none (do not overwrite existing tasks)
				(human.currentQueueTasks == null || human.currentQueueTasks.size() == 0)) {
			human.currentQueueTasks = getTasksFromPriority(grid, human, human.activePriority);
			
			if (human.currentQueueTasks != null) {
				String firstTaskString = human.currentQueueTasks.size() > 0 ? human.currentQueueTasks.get(0).toString() : "tasks is empty";
				CustomLog.outPrintln(human.name + " was assigned " + human.currentQueueTasks.size() + " tasks, first: " + firstTaskString); 
			}
			else {
				CustomLog.outPrintln(human.name + " was assigned NULL tasks"); 
			}
			
			if (human.currentQueueTasks != null && human.currentQueueTasks.size() > 0)
				CustomLog.outPrintln(human.currentQueueTasks.get(0).getClass());
			if (human.currentQueueTasks instanceof DoneTaskPlaceholder ||
					human.currentQueueTasks instanceof ImpossibleTaskPlaceholder) {
				human.activePriority = null;
				human.currentQueueTasks = null;
			}
		}
		
		//Follow through with one step, deconstruct into a priority, and get its status back.
		if (process.processStepIndex >= 0 && process.processStepIndex < process.processSteps.size()) {
			ProcessStep step = process.processSteps.get(process.processStepIndex);
			if (human.activePriority == null) {
				human.activePriority = getPriorityForStep(society, grid, human, human, process, step);
				String priorityName = human.activePriority == null ? "null" : human.activePriority.toString();
				CustomLog.outPrintln(human.name + ", for its process: " + process.toString() + ", ");
						CustomLog.outPrintln("was given the PRIORITY: " + priorityName);
			}
			if (human.activePriority instanceof UnsupervisedDesignation) {
				human.unsupervisedProcesses.add(human.processProgress);
				human.processProgress = null;
				human.activePriority = null;
				human.currentQueueTasks = null;
			}
			if (human.activePriority instanceof DonePriority || 
					human.activePriority instanceof ImpossiblePriority) {
				if (human.activePriority instanceof ImpossiblePriority) {
					CustomLog.errPrintln("Warning, impossible for process step: " + human.processProgress.processSteps.get(0));
					ImpossiblePriority iPrior = (ImpossiblePriority) human.activePriority;
					CustomLog.errPrintln("Warning, impossible priority: " + iPrior.reason);
				}
				human.processProgress.processStepIndex++;
				human.activePriority = null;
			}
			if (human.activePriority instanceof WaitPriority) {
				human.activePriority = null; //Spend a frame and advance priority/step/etc.
			}
		}
		//Process has been completed, remove it from person
		if (process.processStepIndex < 0 || process.processStepIndex >= process.processSteps.size()) {
			process.recRepeats--;
			
			if (process.targetTile != null) {
				if (process.processTile.tileBlockId == ItemData.ITEM_EMPTY_ID) {
					process.targetTile = null;
					process.processTile = null;
				}
			}
			if (process.processTile != null) {
				if (process.processTile.tileBlockId == ItemData.ITEM_EMPTY_ID) {
					process.processTile = null;
				}
			}
			
			if (process.recRepeats <= 0) {
				CustomLog.outPrintln(human.name + " COMPLETED process: " + human.processProgress);
				executeProcessResActions(grid, human, human.processProgress.processResActions);
				human.processProgress = null; //Reset all jobs and job fields to null
				if (process.processBuilding != null) {
					process.processBuilding.currentUser = null;
					process.processBuilding = null;
				}
				if (process.processTile != null || process.targetTile != null) {
					process.processTile.harvestInUse = false;
					process.processTile = null;
					process.targetTile = null;
				}
				assignSingleHumanJob(society, grid, human, date);
			}
			else {
				process.processStepIndex = 0;
			}
			
			calcFinishedProcess(society, grid, human, process);
		}
		
		//Assign a new process. Note that a human may have a priority not linked to any higher structure,
		//which we do not want to override or chain incorrectly to a new job.
		if (human.processProgress == null && human.activePriority == null) {
			assignSingleHumanJob(society, grid, human, date);
		}
		CustomLog.outPrintln();
	}
	
	private static void deconstructionJob(Human human, LocalGrid grid, Society society, Date date, LocalJob job) {
		LocalProcess process = job.jobWorkProcess;
		Set<Human> capitalOwners = new HashSet<Human>() {{
			add(human); 
			if (human.household != null) {
				addAll(human.household.householdMembers);
			}
			add(job.boss);
		}};
		
		//Assign the location of the process where the steps are undertaken
		if (process.requiredBuildNameOrGroup != null && process.processBuilding == null) {
			assignBuilding(grid, human, process, capitalOwners, process.requiredBuildNameOrGroup);
		}
		else if (process.requiredTileNameOrGroup != null && process.processTile == null) {
			assignTile(grid, human, capitalOwners, process);
		}
		
		if (human.currentQueueTasks != null && human.currentQueueTasks.size() > 0) {
			Task task = human.currentQueueTasks.get(0);
			CustomLog.outPrintln(human.name + " has " + human.currentQueueTasks.size() + " tasks."); 
			if (task.taskTime <= 0) {
				CustomLog.outPrintln(human.name + " FINISHED task: " + task.toString());
				human.currentQueueTasks.remove(0);
				executeTask(grid, human, task);
				if (human.currentQueueTasks.size() == 0) {
					human.activePriority = null;
				}
			}
			else {
				task.taskTime--;
				CustomLog.outPrintln(human.name + " task in progress: " 
						+ task.getClass().getSimpleName() + " (" + task.taskTime + ")");
			}
		}
		if (human.activePriority != null && //Assign tasks if there are none (do not overwrite existing tasks)
				(human.currentQueueTasks == null || human.currentQueueTasks.size() == 0)) {
			human.currentQueueTasks = getTasksFromPriority(grid, human, human.activePriority);
			
			if (human.currentQueueTasks != null) {
				String firstTaskString = human.currentQueueTasks.size() > 0 ? human.currentQueueTasks.get(0).toString() : "tasks is empty";
				CustomLog.outPrintln(human.name + " was assigned " + human.currentQueueTasks.size() + " tasks, first: " + firstTaskString); 
			}
			else {
				CustomLog.outPrintln(human.name + " was assigned NULL tasks"); 
			}
			
			if (human.currentQueueTasks != null && human.currentQueueTasks.size() > 0)
				CustomLog.outPrintln(human.currentQueueTasks.get(0).getClass());
			if (human.currentQueueTasks instanceof DoneTaskPlaceholder ||
					human.currentQueueTasks instanceof ImpossibleTaskPlaceholder) {
				human.activePriority = null;
				human.currentQueueTasks = null;
			}
		}
		
		//Follow through with one step, deconstruct into a priority, and get its status back.
		if (process.processStepIndex >= 0 && process.processStepIndex < process.processSteps.size()) {
			ProcessStep step = process.processSteps.get(process.processStepIndex);
			if (human.activePriority == null) {
				human.activePriority = getPriorityForStep(society, grid, 
						human, job.boss, process, step);
				String priorityName = human.activePriority == null ? "null" : human.activePriority.toString();
				CustomLog.outPrintln(human.name + ", for its job process: " + process + ", " +
						"was given the PRIORITY: " + priorityName);
			}
			if (human.activePriority instanceof UnsupervisedDesignation) {
				human.unsupervisedJobs.add(human.jobProcessProgress);
				human.jobProcessProgress = null;
				human.activePriority = null;
				human.currentQueueTasks = null;
			}
			if (human.activePriority instanceof DonePriority || 
					human.activePriority instanceof ImpossiblePriority) {
				if (human.activePriority instanceof ImpossiblePriority) {
					ImpossiblePriority iPrior = (ImpossiblePriority) human.activePriority;
					CustomLog.errPrintln("Warning, impossible priority: " + iPrior.reason);
				}
				process.processStepIndex++;
				human.activePriority = null;
			}
			if (human.activePriority instanceof WaitPriority) {
				human.activePriority = null; //Spend a frame and advance priority/step/etc.
			}
		}
		
		//Process has been completed, remove it from person
		if (process.processStepIndex < 0 || process.processStepIndex >= process.processSteps.size()) { 
			process.recRepeats--;
			
			if (process.targetTile != null) {
				if (process.processTile.tileBlockId == ItemData.ITEM_EMPTY_ID) {
					process.targetTile = null;
					process.processTile = null;
				}
			}
			if (process.processTile != null) {
				if (process.processTile.tileBlockId == ItemData.ITEM_EMPTY_ID) {
					process.processTile = null;
				}
			}
			
			if (process.recRepeats <= 0) {
				executeProcessResActions(grid, human, process.processResActions);
				CustomLog.outPrintln(human.name + " COMPLETED process (as an employee for a job): " + process);
				
				//Special data for jobs
				MapUtil.removeSafeNestSetMap(human.jobProcessProgress.boss.workers, human, human.jobProcessProgress);
				
				human.jobProcessProgress = null; //Reset all jobs and job fields to null
				if (process.processBuilding != null) {
					process.processBuilding.currentUser = null;
					process.processBuilding = null;
				}
				if (process.processTile != null || process.targetTile != null) {
					process.processTile.harvestInUse = false;
					process.processTile = null;
					process.targetTile = null;
				}
			}
			else {
				process.processStepIndex = 0;
			}
			
			calcFinishedProcess(society, grid, human, process);
		}
	}
	
	/**
	 * Fires when any process is completed by a human. 
	 * 
	 * @param society
	 * @param grid
	 * @param human
	 * @param process
	 */
	private static void calcFinishedProcess(Society society, LocalGrid grid, Human human, LocalProcess process) {
		//Change both human and societal ethos of this process
		Map<Integer, Double> goodsProduced = new HashMap<>();
		if (process.outputItems != null)
			goodsProduced = process.outputItems.itemDropExpectation;
		Map<Integer, Double> societalUtil = society.getCalcUtil();  
		double score = MapUtil.dotProductOfMaps(goodsProduced, societalUtil);
		
		Map<String, Integer> actionNamesMap = new HashMap<>();
		if (process.processResActions != null) {
			for (ProcessStep step: process.processResActions) {
				MapUtil.addNumMap(actionNamesMap, step.stepType, step.modifier);
			}
		}
		NeedsGamut needsGamut = society.humanNeedsMap.get(human);
		if (needsGamut == null) needsGamut = society.totalNeedsGamut;
		score += needsGamut.dotProduct(actionNamesMap);
		
		int supervTime = process.totalSupervisedTime();
		score /= supervTime;
		score /= ((process.totalTime() - supervTime) * 0.2);
		
		double logScore = Math.log10(score);
		
		//Change human ethos with calculated scoring/utility
		Ethos ethos = human.brain.ethosSet.ethosTowardsProcesses.get(process);
		if (ethos != null) {
			ethos.severity += logScore;
			ethos.ethosLifetimeHappiness += logScore;
		}
		
		//Create an experience for people in their heads
		LocalExperience processExp = new LocalExperience(process.name);
		processExp.severity = Math.max(1, logScore);
		processExp.beingRoles.put(human, CollectionUtil.newSet("Worker"));
		human.brain.experiences.add(processExp);
	}
	
	

	private static LocalBuilding assignBuilding(LocalGrid grid, LivingEntity being, LocalProcess process,
			Set<Human> capitalOwners, String buildingName) {
		int id = ItemData.getIdFromName(buildingName);
		KdTree<Vector3i> nearestItemsTree = grid.getKdTreeForBuildings(id);
		if (nearestItemsTree == null || nearestItemsTree.size() == 0) return null;
		int numCandidates = Math.min(nearestItemsTree.size(), 10);
		Collection<Vector3i> nearestCoords = nearestItemsTree.nearestNeighbourListSearch(
				numCandidates, being.location.coords);
		for (Vector3i nearestCoord: nearestCoords) {
			LocalBuilding building = grid.getTile(nearestCoord).building;
			if (building != null) {
				if (building.owner == null || capitalOwners.contains(building.owner)) {
					if (building.currentUser == null) {
						
						Set<Vector3i> neighbors = grid.getAllNeighbors14(nearestCoord);
						neighbors.add(nearestCoord);
						for (Vector3i neighbor: neighbors) {
							ScoredPath scoredPath = grid.pathfinder.findPath(
									being, being.location, grid.getTile(neighbor));
							if (scoredPath.isValid()) {
								building.currentUser = being;
								process.processBuilding = building;
							}
						}
						
					}
				}
			}
		}
		return process.processBuilding;
	}
	
	/**
	 * 
	 * The reason why useAboveTileOverride exists, is because of the two cases in required tile:
	 * 
	 * the user is directly harvesting the tile (point directly at needed tile);
	 * the user needs to complete a process on top of a tile (point at one above tile, like in farming).
	 * 
	 * @return The harvest tile, followed by the tile to move to (within <= 1 distance)
	 */
	public static Pair<LocalTile> assignTile(LocalGrid grid, LivingEntity being, 
			Set<Human> validOwners, LocalProcess process) {
		
		String tileName = process.requiredTileNameOrGroup;
		boolean useAboveTileOverride = !process.name.contains("Harvest Tile ");
		
		//int tileId = ItemData.getIdFromName(tileName);
		KdTree<Vector3i> items = grid.getKdTreeForItemGroup(tileName);
		if (items == null) return null;
		
		Collection<Vector3i> candidates = items.nearestNeighbourListSearch(50, being.location.coords);
		
		//Collect pair objects: the actual location of interest, followed by an accessible neighbor
		for (Vector3i candidate: candidates) {
			if (useAboveTileOverride) {
				candidate = candidate.getSum(0, 1, 0);
			}
			
			//CustomLog.outPrintln("Calc path: " + being.location.coords + " to -> " + grid.getTile(candidate).coords);
			LocalTile tile = grid.getTile(candidate);
			
			List<Human> claimants = grid.findClaimantToTile(candidate);
			
			if (claimants == null || claimants.size() == 0 || 
					CollectionUtil.colnsHasIntersect(claimants, validOwners)) {
				if (!tile.harvestInUse) {
					List<Vector3i> neighbors = grid.getAll14NeighborsSorted(candidate, being.location.coords);
					neighbors.add(candidate);
					
					List<LocalTile> listEndGoals = new ArrayList<>();
					for (Vector3i vec: neighbors) listEndGoals.add(grid.getTile(vec));
					//Use batch pathfinder in speeding up this calculation
					Pair<Map<LocalTile, ScoredPath>> allPathingData = grid.pathfinder.batchPathfinding(
							being, being.location, listEndGoals);
					Map<LocalTile, ScoredPath> validPaths = allPathingData.first;
					
					for (Entry<LocalTile, ScoredPath> entry: validPaths.entrySet()) {
						LocalTile dest = entry.getKey();
						ScoredPath scoredPath = grid.pathfinder.findPath(
								being, being.location, dest);
						if (scoredPath.isValid()) {
							Pair<LocalTile> pair = new Pair<>(grid.getTile(candidate), dest);
							process.processTile = pair.second;
							process.targetTile = pair.first;
							pair.first.harvestInUse = true;
							return pair;
						}
					}
					//System.err.println("Tile not reachable: " + candidate);
				}
				else {
					//System.err.println("Tile in use: " + tile.coords);
				}
			}
			else {
				//System.err.println("Tile claimed/no societal access: " + tile.coords);
			}
		}
		
		/*
		tileByPathScore = MapUtil.getSortedMapByValueDesc(tileByPathScore);
		for (Object obj: tileByPathScore.keySet().toArray()) {
			Pair<LocalTile> bestPair = (Pair) obj;
			being.processTile = bestPair.second;
			being.targetTile = bestPair.first;
			bestPair.first.harvestInUse = true;
			return bestPair;
		}
		*/
		
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
		
		Set<Human> validLandOwners = new HashSet<>();
		validLandOwners.add(being); validLandOwners.add(ownerProducts);
		
		Set<LivingEntity> validEntitiesBeings = new HashSet<>();
		validEntitiesBeings.add(being); validEntitiesBeings.add(ownerProducts);
		
		//CustomLog.outPrintln("Figuring out priority, for process: " + process);
		
		//TODO;
		//Use the battle class and advance it with the combatengine either here or in global world ticking,
		//when the necessary conditions are met, usually at least two hostile people in proximity with one another.
		//Also for hunting priorities and situations where one party can initiate a fight.
		
		if (process.name.equals("Build Basic Home")) {
			int width = (int) Math.ceil(Math.sqrt(being.ownedBuildings.size() + being.inventory.size() + 8));
			Vector2i requiredSpace = new Vector2i(width, width);
			
			List<GridRectInterval> bestRectangles = findBestOpenRectSpace(
					grid, society, validLandOwners, being.location.coords, requiredSpace);
				
			if (bestRectangles != null && bestRectangles.size() > 0) {
				LinkedHashSet<Vector3i> borderRegion = VecGridUtil.getBorderRegionFromCoords(
						Vector3i.getRange(bestRectangles));
				Set<Integer> bestBuildingMaterials = society.getBestBuildingMaterials(society.calcUtility, 
						being, borderRegion.size());
				
				//Use land claims on the spaces not already claimed
				for (GridRectInterval interval: bestRectangles) {
					List<Human> claimants = grid.findClaimantToTiles(interval);
					if (claimants == null || claimants.size() == 0) {
						grid.claimTiles(ownerProducts, interval.getStart(), interval.getEnd(), process);
					}
				}
				
				Vector3i singleLocation = bestRectangles.iterator().next().avgVec();
				PurposeAnnotatedBuild compound = new PurposeAnnotatedBuild("Home", singleLocation);
				compound.addRoom("Bedroom", bestRectangles, borderRegion, 
						VecGridUtil.setUnderVecs(grid, Vector3i.getRange(bestRectangles)));
				MapUtil.insertNestedListMap(ownerProducts.designatedBuildsByPurpose, "Home", compound);
				
				priority = new ConstructRoomPriority(borderRegion, bestBuildingMaterials);
			}
			else {
				priority = new ImpossiblePriority("Could not find open rectangular space");
			}
			return priority;
		}
		else if (process.name.equals("Improve Complex")) {
			//Choose the best complex and room to expand for this human, if necessary
			//Also use this process when there's a need to build a new room
			//Like for example, household needs food. Create a kitchen that has an oven and stores food.
			Map<Human, NeedsGamut> societyNeeds = society.humanNeedsMap != null ? society.humanNeedsMap : 
				society.findAllNeedsIntensityMap(); 
			if (societyNeeds.containsKey(being)) {
				NeedsGamut humanNeeds = societyNeeds.get(being);
				return AnnoRoomProcDeconst.improveComplex(grid, society, 
						being, ownerProducts, validLandOwners, validEntitiesBeings, humanNeeds);
			}
		}
		else if (process.name.equals("Local Soldier Duty")) {
			return new SoldierPriority(society.societyCenter, being);
		}
		else if (process.name.equals("Put On Clothes")) {
			return progressToFindItemGroups(grid, being, validEntitiesBeings, 
					new HashMap<String, Integer>() {{put("Clothing", 1);}},
					new ItemMetricsUtil.AvailClothingMetric()
					);
		}
		
		Vector3i primaryLocation = null;
		Inventory destinationInventory = null;
		String itemMode = "";
		if (process.processBuilding != null) {
			primaryLocation = process.processBuilding.getPrimaryLocation();
			destinationInventory = process.processBuilding.inventory;
			itemMode = "Building";
		}
		else if (process.processTile != null) {
			primaryLocation = process.processTile.coords;
			destinationInventory = being.inventory;
			itemMode = "Tile";
		}
		else if (process.requiredBuildNameOrGroup == null && process.requiredTileNameOrGroup == null) {
			primaryLocation = being.location.coords;
			destinationInventory = being.inventory;
			itemMode = "Personal";
		}
		else {
			if (process.requiredTileNameOrGroup != null && process.processTile == null) {
				return progressToFindItemGroups(grid, being, validEntitiesBeings, 
						new HashMap<String, Integer>() {{put(process.requiredTileNameOrGroup, 1);}},
						new ItemMetricsUtil.DefaultItemMetric()
						);
			}
			
			CustomLog.errPrintln("Warning, building and/or tile required: " + 
					process.toString() + ", tile/building may not be assigned");
			return new ImpossiblePriority("Warning, could not find case for process. Tile/building may not be assigned");
		}
		
		Vector3i targetLocation = primaryLocation;
		if (process.targetTile != null)
			targetLocation = process.targetTile.coords;
		
		//For the case where a person has not been assigned a building/space yet
		if ((primaryLocation == null && process.requiredBuildNameOrGroup != null)
				|| process.isCreatedAtSite) {
			CustomLog.outPrintln("Find status building case");
			CustomLog.outPrintln(process.requiredBuildNameOrGroup);
			CustomLog.outPrintln(process);
			
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
			
			requiredSpace = requiredSpace.getSum(2,2);
			
			//If available building, use it, 
			//otherwise find space needed for building, allocate it, and start allocating a room
			GridRectInterval nearOpenSpace = grid.getNearestViableRoom(society, validLandOwners, 
					being.location.coords, requiredSpace);

			if (nearOpenSpace == null) { //No available rooms to use
				List<GridRectInterval> bestRectangles = findBestOpenRectSpace(grid, society, validLandOwners, 
						being.location.coords, requiredSpace);
				
				if (bestRectangles != null && bestRectangles.size() > 0) {
					Set<Vector3i> bestRectangleVecs = new HashSet<>();
					Iterator<Vector3i> iter = Vector3i.getRange(bestRectangles);
					iter.forEachRemaining(bestRectangleVecs::add);
					
					LinkedHashSet<Vector3i> borderRegion = VecGridUtil.getBorderRegionFromCoords(
							Vector3i.getRange(bestRectangles));
					
					CustomLog.errPrintln("Create building space: " + bestRectangles.toString());
					Set<Integer> bestBuildingMaterials = society.getBestBuildingMaterials(society.calcUtility, 
							being, (requiredSpace.x + requiredSpace.y) * 2);
					
					//Use land claims on the spaces not already claimed
					for (GridRectInterval interval: bestRectangles) {
						List<Human> claimants = grid.findClaimantToTiles(interval);
						if (claimants == null || claimants.size() == 0) {
							grid.claimTiles(ownerProducts, interval.getStart(), interval.getEnd(), process);
						}
					}
					
					grid.setInUseRoomSpace(bestRectangleVecs, true);
					
					PurposeAnnotatedBuild compound = new PurposeAnnotatedBuild("Home", 
							bestRectangleVecs.iterator().next());
					compound.addRoom("Bedroom", bestRectangles, borderRegion, 
							VecGridUtil.setUnderVecs(grid, Vector3i.getRange(bestRectangles)));
					MapUtil.insertNestedListMap(ownerProducts.designatedBuildsByPurpose, "Home", compound);
					
					priority = new ConstructRoomPriority(borderRegion, bestBuildingMaterials);
				}
				else {
					CustomLog.outPrintln("Could not create building space of size: " + requiredSpace);
					priority = new ImpossiblePriority("Could not create building space of size: " + requiredSpace);
				}
				return priority;
			}
			else {
				CustomLog.errPrintln("Assigned building space: " + nearOpenSpace.toString());
				grid.setInUseRoomSpace(nearOpenSpace, true);
				
				Vector3i newBuildCoords = nearOpenSpace.avgVec();
				if (process.isCreatedAtSite) {
					process.processTile = grid.getTile(newBuildCoords);
					if (being.location.coords.areAdjacent14(targetLocation)) {
						//continue
					}
					else {
						return new MovePriority(primaryLocation);
					}
				}
				else { //implies process.requiredBuildNameOrGroup != null -> buildingId is valid
					priority = new BuildingPlacePriority(newBuildCoords, 
							process.requiredBuildNameOrGroup, ownerProducts);
					return priority;
				}
			}
		}
		
		if (step.stepType.equals("sleepInBed")) {
			if (being.location.coords.areAdjacent14(primaryLocation)) {
				return new SleepInBedPriority();
			}
			else {
				priority = new MoveTolDistOnePriority(primaryLocation);
			}
		}
		if (step.stepType.equals("I")) {
			if (process.processBuilding != null && process.processBuilding.inventory.hasItems(process.inputItems)) {
				process.processBuilding.inventory.subtractItems(process.inputItems);
				return new DonePriority();
			}
			else if (destinationInventory.hasItems(process.inputItems) && itemMode.equals("Personal")) {
				being.inventory.subtractItems(process.inputItems);
				return new DonePriority();
			}
			else if (destinationInventory.hasItems(process.inputItems) && process.processTile != null) {
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

				List<InventoryItem> regularItemNeeds = (List) invData[2];
				CustomLog.outPrintln("Searching for: " + regularItemNeeds);

				if (regularItemNeeds.size() == 0) {
					return new DonePriority();
				}
				
				InventoryItem chosenItem = regularItemNeeds.get(0);
				int firstItemNeeded = chosenItem.itemId;
				int amountNeeded = chosenItem.quantity;
				
				return progressToFindItem(grid, being, new HashSet<LivingEntity>() {{add(being);}}, 
						new HashMap<Integer, Integer>() {{put(firstItemNeeded, amountNeeded);}}, 
						new DefaultItemMetric());
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
				
				String skillName = step.stepType.substring(1);
				being.skillBook.addExperienceToSkill(skillName, 1);
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
		else if (step.stepType.startsWith("Fish")) {
			if (being.location.coords.areAdjacent14(primaryLocation)) {
				List<InventoryItem> outputItems = process.outputItems.getOneItemDrop();
				for (InventoryItem item: outputItems) {
					item.owner = ownerProducts;
				}
				return new WaitPriority();
			}
			else {
				priority = new MoveTolDistOnePriority(primaryLocation);
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
				return new UnsupervisedDesignation();
			}
		}
		else if (step.stepType.equals("O") || step.stepType.equals("OArtwork") || step.stepType.equals("D")) {
			if (step.stepType.equals("D")) {
				if (process.processBuilding != null) {
					grid.removeBuilding(process.processBuilding);
				}
			}
			
			List<InventoryItem> outputItems = process.outputItems.getOneItemDrop();
			for (InventoryItem item: outputItems) {
				item.owner = ownerProducts;
				ownerProducts.ownedItems.add(item);
				
				//Factor in ethos, skill, person, and later, technology, into the output quality and quantity of goods
				//Also provide a skill based distribution of goods in the process CSV
				ItemQuality[] qualityArr = ItemQuality.values();
				NormalDistribution normal = new NormalDistribution(qualityArr.length / 2.0, qualityArr.length / 8.0);
				int sample = (int) normal.sample();
				int indexChosen = (int) MathAndDistrUti.clamp(sample, 0, qualityArr.length - 1);
				ItemQuality quality = qualityArr[indexChosen];
				item.quality = quality;
			}
			if (step.stepType.equals("OArtwork")) { //All output items are given art status and memory
				for (InventoryItem item: outputItems) {
					ArtworkGraph artGraph = ArtworkGraph.generateRandArtGraph(being, null);
					item.itemSpecProperties.addProperty(new ItemProperty.ItemArtProperty(
							StringUtil.genAlphaNumericStr(20), being, StringUtil.genAlphaNumericStr(20), 
							ItemQuality.NORMAL, artGraph));
				}
			}
			
			CustomLog.outPrintln("Dropped items: " + new Inventory(outputItems) + " at place: " + itemMode);
			
			
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
			
			CustomLog.outPrintln("Built building: " + building.name + " at place: " + itemMode);
			
			Vector3i aboveTargetLocation = targetLocation.getSum(0, 1, 0);
			if (grid.inBounds(aboveTargetLocation) || grid.tileIsFullAccessible(aboveTargetLocation)) {
				grid.addBuilding(building, targetLocation, true, ownerProducts);
				return new DonePriority();
			}
			else {
				return new ImpossiblePriority("Above target tile not in bounds or accessible");
			}
		}
		
		if (!step.stepType.startsWith("U") && priority == null)
			CustomLog.errPrintln("Warning, case not covered for priority from step, returning null priority");
		return priority;
	}
	
	private static Priority getPriorForStepUnsupervBasic(LocalGrid grid, LivingEntity unsupervHuman, 
			LocalProcess process, ProcessStep step) {
		Priority priority = null;
		
		if (process.name.equals("Wander Around")) {
			Vector3i target = grid.getRandomNearAccessTile(unsupervHuman.location.coords, 10);
			return new MovePriority(target);
		}
		
		Vector3i primaryLocation = null;
		String itemMode = "";
		Inventory destinationInventory = null;
		if (process.processBuilding != null) {
			primaryLocation = process.processBuilding.getPrimaryLocation();
			destinationInventory = process.processBuilding.inventory;
			itemMode = "Building";
		}
		else if (process.processTile != null) {
			primaryLocation = process.processTile.coords;
			destinationInventory = process.processTile.itemsOnFloor;
			itemMode = "Tile";
		}
		else if (process.requiredBuildNameOrGroup == null && process.requiredTileNameOrGroup == null) {
			primaryLocation = unsupervHuman.location.coords;
			destinationInventory = unsupervHuman.inventory;
			itemMode = "Personal";
		}
		else {
			CustomLog.errPrintln("Warning, building and/or tile required: " + 
					process.toString() + ", tile/building may not be assigned");
			return new ImpossiblePriority("Warning, could not find destination for unsupervised process.");
		}

		if (step.stepType.startsWith("Wait")) {
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
				return new UnsupervisedDesignation();
			}
		}
		else if (step.stepType.equals("O") || step.stepType.equals("OArtwork") || step.stepType.equals("D")) {
			if (step.stepType.equals("D")) {
				if (process.processBuilding != null) {
					grid.removeBuilding(process.processBuilding);
					
					//TODO; //Fix "D" so that it factors into process backprop
				}
			}
			List<InventoryItem> outputItems = process.outputItems.getOneItemDrop();
			for (InventoryItem item: outputItems) {
				item.owner = unsupervHuman;
			}
			if (step.stepType.equals("OArtwork")) { //All output items are given art status and memory
				if (unsupervHuman instanceof Human) {
					Human artist = (Human) unsupervHuman;
					for (InventoryItem item: outputItems) {
						ArtworkGraph artGraph = ArtworkGraph.generateRandArtGraph(artist, null);
						item.itemSpecProperties.addProperty(new ItemProperty.ItemArtProperty(
								StringUtil.genAlphaNumericStr(20), artist, StringUtil.genAlphaNumericStr(20), 
								ItemQuality.NORMAL, artGraph));
					}
				}
			}
			
			CustomLog.outPrintln("Dropped items: " + new Inventory(outputItems) + " at place: " + itemMode);
			
			if (unsupervHuman.inventory.canFitItems(outputItems) && (itemMode.equals("Personal") || itemMode.equals("Tile"))) {
				unsupervHuman.inventory.addItems(outputItems);
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
		
		if (!step.stepType.startsWith("U") && priority == null)
			CustomLog.errPrintln("Warning, case not covered for priority from step, returning null priority");
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
				return new DoneTaskPlaceholder();
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
				return new DoneTaskPlaceholder();
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
				return new DoneTaskPlaceholder();
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
				return new DoneTaskPlaceholder();
			}
			else {
				return getTasksFromPriority(grid, being, new MoveTolDistOnePriority(itemPriority.coords));
			}
		}
		else if (priority instanceof TileHarvestPriority) {
			TileHarvestPriority tilePriority = (TileHarvestPriority) priority;
			LocalTile hTile = grid.getTile(tilePriority.coords);
			if (hTile == null || hTile.tileBlockId == ItemData.ITEM_EMPTY_ID) {
				return new DoneTaskPlaceholder();
			}
			if (tilePriority.coords.areAdjacent14(being.location.coords)) {
				int pickupTime = ItemData.getPickupTime(hTile.tileBlockId);
				tasks.add(new HarvestBlockTileTask(pickupTime, tilePriority.coords));
			}
			else {
				return getTasksFromPriority(grid, being, new MoveTolDistOnePriority(tilePriority.coords));
			}
		}
		else if (priority instanceof TilePlacePriority) {
			TilePlacePriority tilePriority = (TilePlacePriority) priority;
			LocalTile tile = grid.getTile(tilePriority.coords);
			
			if (tile != null && tile.tileBlockId == tilePriority.materialId) {
				return new DoneTaskPlaceholder();
			}
			if (tilePriority.coords.areAdjacent14(being.location.coords)) {
				int placeTime = 10;
				tasks.add(new PlaceBlockTileTask(placeTime, tilePriority.coords, tilePriority.materialId));
			}
			else {
				return getTasksFromPriority(grid, being, new MoveTolDistOnePriority(tilePriority.coords));
			}
		}
		else if (priority instanceof BuildingPlacePriority) {
			BuildingPlacePriority buildPriority = (BuildingPlacePriority) priority;
			
			if (buildPriority.coords.areAdjacent14(being.location.coords)) {
				LocalTile tile = grid.getTile(buildPriority.coords);
				if (tile.building != null) {
					CustomLog.errPrintln("Warning, was given command to place building where one already exists");
					return new ImpossibleTaskPlaceholder();
				}
				
				if (being.inventory.findItemCountGroup(buildPriority.buildingName) >= 1 &&
						tile.building == null) {
					Set<InventoryItem> items = being.inventory.subtractItem(
							ItemData.item(buildPriority.buildingName, 1));
					if (items == null || items.size() == 0) return new ImpossibleTaskPlaceholder();
					InventoryItem item = items.iterator().next();
					LocalBuilding newBuilding = ItemData.building(item.itemId);
					grid.addBuilding(newBuilding, buildPriority.coords, false, buildPriority.owner);
					return new DoneTaskPlaceholder();
				}
				else {
					Priority itemSearchPriority = progressToFindItemGroups(grid, being, new HashSet<LivingEntity>() {{add(being);}}, 
							new HashMap<String, Integer>() {{
								put(buildPriority.buildingName, 1);
							}}, 
							new DefaultItemMetric());
					return getTasksFromPriority(grid, being, itemSearchPriority);
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
				return new ImpossibleTaskPlaceholder();
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
					progressToFindItemGroups(grid, being, new HashSet<LivingEntity>() {{add(being);}}, 
							new HashMap<String, Integer>() {{put("Food", 1);}}, new FoodMetric())
					);
		}
		else if (priority instanceof RestPriority) {
			being.rest(1);
			if (being.rest >= being.maxRest * 0.8) {
				return new DoneTaskPlaceholder();
			}
			else {
				return tasks;
			}
		}
		else if (priority instanceof ConstructRoomPriority) {
			ConstructRoomPriority consPriority = (ConstructRoomPriority) priority;
			
			Vector3i bestLocation = null;
			while (bestLocation == null || !grid.tileIsPartAccessible(bestLocation)) {
				if (consPriority.remainingBuildCoords.size() == 0) {
					grid.setInUseRoomSpace(consPriority.allBuildingCoords, true);
					return new DoneTaskPlaceholder();
				}
				bestLocation = consPriority.remainingBuildCoords.iterator().next();
				consPriority.remainingBuildCoords.remove(bestLocation);
			}
			
			Collection<Integer> rankedMaterials = consPriority.rankedBuildMaterials;
			for (int materialId: rankedMaterials) {
				if (being.inventory.findItemCount(materialId) > 0) {
					return getTasksFromPriority(grid, being, new TilePlacePriority(bestLocation, materialId));
				}
			}
			
			for (int materialId: rankedMaterials) {
				Priority itemSearchPriority = progressToFindItem(grid, being, 
						new HashSet<LivingEntity>() {{add(being);}}, 
						new HashMap<Integer, Integer>() {{
							put(materialId, consPriority.remainingBuildCoords.size());
						}}, 
						new DefaultItemMetric());
				
				if (!(itemSearchPriority instanceof ImpossiblePriority)) {
					return getTasksFromPriority(
							grid, being,
							itemSearchPriority
							);
				}
			}
			
			return new ImpossibleTaskPlaceholder();
		}
		else if (priority instanceof ImproveRoomPriority) {
			ImproveRoomPriority improveRoomPriority = (ImproveRoomPriority) priority;
			AnnotatedRoom room = improveRoomPriority.room;
			Set<LivingEntity> validEntities = improveRoomPriority.validEntitiesOwners;
			
			Priority newResultPriority = null; 
			Map<String, Integer> chosenNeedsMap = null;
			boolean isBuildingCase = false;
			
			Map<String, Integer> desiredBuildCounts = CollectionUtil.searchMapCondByValue(
					room.desiredBuildCounts, (x) -> x > 0);
			if (desiredBuildCounts.size() > 0) {
				newResultPriority = LocalGridTimeExecution.progressToFindItemGroups(grid, being, validEntities, 
						desiredBuildCounts, new ItemMetricsUtil.DefaultItemMetric());
				chosenNeedsMap = room.desiredBuildCounts;
				isBuildingCase = true;
			}
			
			if (newResultPriority == null) {
				Map<String, Integer> desiredItemStorage = CollectionUtil.searchMapCondByValue(
						room.desiredItemStorage, (x) -> x > 0);
				if (desiredItemStorage.size() > 0) {
					newResultPriority = LocalGridTimeExecution.progressToFindItemGroups(grid, being, validEntities, 
							desiredItemStorage, new ItemMetricsUtil.DefaultItemMetric());
					chosenNeedsMap = room.desiredItemStorage;
					isBuildingCase = false;
				}
			}
			
			if (newResultPriority == null) {
				Map<String, Integer> desiredBuildCountsOpt = CollectionUtil.searchMapCondByValue(
						room.desiredBuildCountsOpt, (x) -> x > 0);
				if (desiredBuildCountsOpt.size() > 0) {
					newResultPriority = LocalGridTimeExecution.progressToFindItemGroups(grid, being, validEntities, 
							desiredBuildCountsOpt, new ItemMetricsUtil.DefaultItemMetric());
					chosenNeedsMap = room.desiredBuildCountsOpt;
					isBuildingCase = true;
				}
			}
			
			if (newResultPriority == null) {
				Map<String, Integer> desiredItemStorageOpt = CollectionUtil.searchMapCondByValue(
						room.desiredItemStorageOpt, (x) -> x > 0);
				if (desiredItemStorageOpt.size() > 0) {
					newResultPriority = LocalGridTimeExecution.progressToFindItemGroups(grid, being, validEntities, 
							desiredItemStorageOpt, new ItemMetricsUtil.DefaultItemMetric());
					chosenNeedsMap = room.desiredItemStorageOpt;
					isBuildingCase = false;
				}
			}
			
			if (newResultPriority instanceof DonePriority) {
				if (isBuildingCase) {
					newResultPriority = new PlaceBuildingAnnoRoomPriority(room, chosenNeedsMap);
				} else {
					newResultPriority = new PlaceItemAnnoRoomPriority(room, chosenNeedsMap);
				}
			}
			
			//Need to progress towards finding items, or placing the items down within the room
			return getTasksFromPriority(grid, being, newResultPriority);
		}
		else if (priority instanceof PlaceBuildingAnnoRoomPriority) {
			PlaceBuildingAnnoRoomPriority placePriority = (PlaceBuildingAnnoRoomPriority) priority;
			AnnotatedRoom room = placePriority.room;
			
			for (GridRectInterval interval: room.fullRoom) {
				Iterator<Vector3i> rangeOfVecs = interval.getRange();
				while (rangeOfVecs.hasNext()) {
					Vector3i vec = rangeOfVecs.next();
					if (room.reservedBuiltSpace.containsKey(vec) && room.reservedBuiltSpace.get(vec) != null) {
						continue;
					}
					else if (!grid.tileIsFullAccessible(vec)) {
						continue;
					}
					else {
						Map<String, Integer> buildings = placePriority.chosenNeedsMap;
						String chosenBuilding = MapUtil.randChoiceFromMapUniform(buildings);
						return getTasksFromPriority(grid, being, 
								new BuildingPlacePriority(vec, chosenBuilding, being));
					}
				}
			}
			
			return new ImpossibleTaskPlaceholder("Room has no accessible places to put buildings");
		}
		else if (priority instanceof PlaceItemAnnoRoomPriority) {
			PlaceItemAnnoRoomPriority placePriority = (PlaceItemAnnoRoomPriority) priority;
			AnnotatedRoom room = placePriority.room;
			
			for (GridRectInterval interval: room.fullRoom) {
				Iterator<Vector3i> rangeOfVecs = interval.getRange();
				while (rangeOfVecs.hasNext()) {
					Vector3i vec = rangeOfVecs.next();
					if (room.reservedBuiltSpace.containsKey(vec) && room.reservedBuiltSpace.get(vec) != null) {
						continue;
					}
					else if (!grid.tileIsFullAccessible(vec)) {
						continue;
					}
					else {
						Map<String, Integer> items = placePriority.chosenNeedsMap;
						String chosenItem = MapUtil.randChoiceFromMapUniform(items);
						Inventory inventory = new Inventory();
						inventory.addItem(ItemData.item(chosenItem, items.get(chosenItem)));
						return getTasksFromPriority(grid, being, 
								new ItemDeliveryPriority(vec, inventory));
					}
				}
			}
			
			return new ImpossibleTaskPlaceholder("Room has no accessible places to put items");
		}
		else if (priority instanceof MovePriority) {
			MovePriority movePriority = (MovePriority) priority;
			
			if (movePriority.coords.equals(being.location.coords)) { 
				return new DoneTaskPlaceholder();
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
				CustomLog.errPrintln("Warning, path was not valid from " + being.location + ", " + grid.getTile(movePriority.coords));
				return new ImpossibleTaskPlaceholder();
			}
		}
		else if (priority instanceof MoveTolDistOnePriority) {
			MoveTolDistOnePriority movePriority = (MoveTolDistOnePriority) priority;
			
			if (movePriority.coords.areAdjacent14(being.location.coords)) { 
				return new DoneTaskPlaceholder();
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
			
			return new ImpossibleTaskPlaceholder();
		}
		else if (priority instanceof SoldierPriority) {
			//SoldierPriority soldierPriority = (SoldierPriority) priority;
			
			if (!(being instanceof Human)) {
				return new ImpossibleTaskPlaceholder();
			}
			
			Human human = (Human) being;
			Society society = human.society;
			
			try {
			
				double weaponAmt = society.groupItemRarity.containsKey("Weapon") ? society.groupItemRarity.get("Weapon") : 0;
				double armorAmt = society.groupItemRarity.containsKey("Armor") ? society.groupItemRarity.get("Armor") : 0;
				
				double weaponReq = SoldierPriority.weaponAmtRequired(being);
				double armorReq = SoldierPriority.armorAmtRequired(being);
				
				Priority resultPriority = null;
				
				if (armorReq > 0 && armorAmt > 0) {
					resultPriority = progressToFindItemGroups(grid, being, new HashSet<LivingEntity>() {{add(being);}}, 
							new HashMap<String, Integer>() {{put("Armor", 1);}}, new DefaultItemMetric());
					if (!(resultPriority instanceof ImpossiblePriority)) {
						return getTasksFromPriority(grid, being, resultPriority);
					}				
				}
				if (weaponReq > 0 && weaponAmt > 0) {
					resultPriority = progressToFindItemGroups(grid, being, new HashSet<LivingEntity>() {{add(being);}}, 
							new HashMap<String, Integer>() {{put("Weapon", 1);}}, new DefaultItemMetric());
					if (!(resultPriority instanceof ImpossiblePriority)) {
						return getTasksFromPriority(grid, being, resultPriority);
					}
				}
				
				resultPriority = new PatrolPriority(society.importantLocations);
				return getTasksFromPriority(grid, being, resultPriority);
			
			} catch (Exception e) {
				System.err.println(human);
				System.err.println(society);
				System.err.println(society.groupItemRarity);
				throw new RuntimeException();
			}
		}
		else if (priority instanceof PatrolPriority) {
			PatrolPriority patrolPriority = (PatrolPriority) priority;
			List<Vector3i> locations = patrolPriority.locations;
			if (locations.size() == 0) {
				return new DoneTaskPlaceholder();
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
			return new DoneTaskPlaceholder();
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
			//TODO
		}
		else if (action.stepType.equals("Artwork")) {
			
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
		else if (task instanceof PlaceBlockTileTask) {
			PlaceBlockTileTask tileTask = (PlaceBlockTileTask) task;
			grid.putBlockIntoTile(tileTask.placeCoords, tileTask.tileId);
		}
	}
	
	private static void assignAllHumanJobs(Society society, LocalGrid grid, Date date) {
		List<Human> humans = society.getAllPeople();
		for (Human human: humans) {
			assignSingleHumanJob(society, grid, human, date);
		}
	}
	private static void assignSingleHumanJob(Society society, LocalGrid grid, Human human, Date date) {
		boolean moveJobQueue = false, moveProcQueue = false;
		if (human.queuedJobs != null && human.queuedJobs.size() > 0) {
			LocalJob job = human.queuedJobs.remove(0);
			human.jobProcessProgress = job;
			if (job instanceof LocalSocietyJob) {
				
			}
			else
				MapUtil.insertNestedSetMap(job.boss.workers, human, job);
			moveJobQueue = true;
		}
		if (human.queuedProcesses != null && human.queuedProcesses.size() > 0) {
			LocalProcess process = human.queuedProcesses.remove(0);
			human.processProgress = process;
			moveProcQueue = true;
		}
		
		if (!moveJobQueue && !moveProcQueue) {
			Map<Integer, Double> calcUtility = society.findCompleteUtilityAllItems(human);
			Map<LocalProcess, Double> bestProcesses = society.prioritizeProcesses(
					calcUtility, grid, human, NUM_JOBPROCESS_CONSIDER, null);
			Map<LocalJob, Double> bestJobs = society.prioritizeJobs(human, bestProcesses, date);
			for (Entry<LocalProcess, Double> entry: bestProcesses.entrySet()) {
				CustomLog.outPrintln(entry.getKey().name + " " + entry.getValue());
			}
			
			Object potentialItem = MapUtil.randChoiceFromMaps(bestProcesses, bestJobs);
			if (potentialItem instanceof LocalJob && human.jobProcessProgress == null) {
				LocalJob potentialJob = (LocalJob) potentialItem;
				human.jobProcessProgress = potentialJob;
				if (potentialJob instanceof LocalSocietyJob) {
					
				}
				else
					MapUtil.insertNestedSetMap(potentialJob.boss.workers, human, potentialJob);
				human.jobProcessProgress.jobWorkProcess.applySkillProcessDistr(human);
			}
			else if (potentialItem instanceof LocalProcess && human.processProgress == null) {
				human.processProgress = (LocalProcess) potentialItem;
				human.processProgress.applySkillProcessDistr(human);
			}
		}
	}
	
	private static void collectivelyAssignJobsSociety(Society society) {
		Map<Integer, Double> calcUtility = society.calcUtility;
		Map<LocalProcess, Double> bestProcesses = society.prioritizeProcesses(
				calcUtility, null, null, NUM_JOBPROCESS_CONSIDER, null);
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
	
	public static boolean hasAccessToItem(LocalGrid grid, LivingEntity being, 
			Map<Integer, Integer> itemsAmtNeeded) {
		for (Entry<Integer, Integer> entry: itemsAmtNeeded.entrySet()) {
			int itemId = entry.getKey();
			int itemAmtNeeded = entry.getValue();
			
			KdTree<Vector3i> nearestItemsTree = grid.getKdTreeForItemId(itemId);
			int recordsFound = nearestItemsTree == null ? 0 : nearestItemsTree.size();
			//TODO: Use item numbers in counting for viable resources
			
			if (nearestItemsTree == null) continue;
				
			int numCandidates = Math.min(nearestItemsTree.size(), 10);
			Collection<Vector3i> nearestCoords = nearestItemsTree.nearestNeighbourListSearch(
					numCandidates, being.location.coords);
			
			boolean foundPath = false;
			for (Vector3i coords: nearestCoords) {
				LocalTile tile = grid.getTile(coords);
				if (tile.harvestInUse) continue;
				
				//Compute a path from the being's location to this tile
				if (grid.pathfinder.hasValidPath(being, being.location.coords, coords)) {
					foundPath = true;
					break;
				}
			}
			if (!foundPath) {
				return false;
			}
		}
		return true;
	}
	
	public static Priority progressToFindItem(LocalGrid grid, 
			LivingEntity being, Set<LivingEntity> owners,
			Map<Integer, Integer> itemsAmtNeeded,
			DefaultItemMetric scoringMetric) {
		
		double bestScore = -1;
		int bestItemIdToFind = -1;
		Vector3i bestLocation = null;

		List<InventoryItem> requiredItems = new ArrayList<>();
		for (Entry<Integer, Integer> entry: itemsAmtNeeded.entrySet()) {
			requiredItems.add(ItemData.createItem(entry.getKey(), entry.getValue()));
		}
		if (being.inventory.hasItems(requiredItems)) {
			return new DonePriority();
		}
			
		//Sort amounts needed by item heuristic and item record counts
		Map<Integer, Double> sortedItemIdsByScore = new HashMap<>();
		for (Entry<Integer, Integer> entry: itemsAmtNeeded.entrySet()) {
			int itemId = entry.getKey();
			int itemAmtNeeded = entry.getValue();
			
			KdTree<Vector3i> nearestItemsTree = grid.getKdTreeForItemId(itemId);
			int recordsFound = nearestItemsTree == null ? 0 : nearestItemsTree.size();
			//List<Vector3i> allCoords = nearestItemsTree.getAllItems();
			
			sortedItemIdsByScore.put(itemId, (double) (recordsFound * itemAmtNeeded));
		}
		sortedItemIdsByScore = MapUtil.getSortedMapByValueDesc(sortedItemIdsByScore);
		
		
		for (Entry<Integer, Double> entry: sortedItemIdsByScore.entrySet()) {
			int firstItemNeeded = entry.getKey();
			
			KdTree<Vector3i> nearestItemsTree = grid.getKdTreeForItemId(firstItemNeeded);
			if (nearestItemsTree == null || nearestItemsTree.size() == 0) continue;
			
			int numCandidates = Math.min(nearestItemsTree.size(), 10);
			Collection<Vector3i> nearestCoords = nearestItemsTree.nearestNeighbourListSearch(
					numCandidates, being.location.coords);

			for (Vector3i itemCoords: nearestCoords) {
				LocalTile tile = grid.getTile(itemCoords);
				if (tile.harvestInUse) continue;
				
				double score = scoringMetric.score(being, owners, grid, tile, 
						itemsAmtNeeded, null);
				
				if (score < bestScore || bestItemIdToFind == -1) {
					bestItemIdToFind = firstItemNeeded;
					bestScore = score;
					bestLocation = itemCoords;
				}
			}	
		}
		if (bestLocation != null) {
			InventoryItem itemClone = new InventoryItem(bestItemIdToFind, 
					itemsAmtNeeded.get(bestItemIdToFind), ItemData.getNameFromId(bestItemIdToFind));
			return new ItemPickupPriority(bestLocation, itemClone);
		}
		
		for (Entry<Integer, Integer> entry: itemsAmtNeeded.entrySet()) {
			int firstItemNeeded = entry.getKey();
			int amountNeeded = entry.getValue();
			
			List<LocalProcess> outputItemProcesses = ProcessData.getProcessesByOutput(firstItemNeeded);
			for (LocalProcess process: outputItemProcesses) {
				KdTree<Vector3i> itemTree = null;
				double effectiveNum = 0;
				
				//double pickupTime = ItemData.getPickupTime(inputTileId);
				double expectedNumItem = process.outputItems.itemExpectation().get(firstItemNeeded);
				
				if (process.name.startsWith("Harvest Tile ")) {
					CustomLog.outPrintln(process);
					Set<Integer> inputTileIds = ItemData.getIdsFromNameOrGroup(process.requiredTileNameOrGroup);
					effectiveNum = Math.min(amountNeeded, expectedNumItem);
					for (int inputTileId: inputTileIds) {
						itemTree = grid.getKdTreeForTile(inputTileId);
						if (itemTree != null && itemTree.size() > 0) break; 
					}
				}
				else if (process.name.startsWith("Harvest Building ")) {
					int buildId = ItemData.getIdFromName(process.requiredBuildNameOrGroup);
					effectiveNum = Math.min(amountNeeded, expectedNumItem);
					itemTree = grid.getKdTreeForBuildings(buildId);
				}
				
				if (itemTree == null || itemTree.size() == 0 || effectiveNum == 0) continue;
				int numCandidates = Math.min(itemTree.size(), 20);
				Collection<Vector3i> nearestCoords = itemTree.nearestNeighbourListSearch(
						numCandidates, being.location.coords);
				for (Vector3i itemCoords: nearestCoords) {
					LocalTile tile = grid.getTile(itemCoords);
					List<Human> claimants = grid.findClaimantToTile(itemCoords);
					if (CollectionUtil.colnsHasIntersect(claimants, owners) && !tile.harvestInUse) {
						int distUtil = being.location.coords.manhattanDist(itemCoords);
						if (bestLocation == null || distUtil < bestScore) {
							bestLocation = itemCoords;
							bestScore = distUtil - effectiveNum;
						}
					}
				}
			}
		}
		if (bestLocation != null) {
			return new TileHarvestPriority(bestLocation);
		}
		return new ImpossiblePriority("Could not find items or processes that can output harvest");
	}
	
	/**
	 * 
	 * @param grid
	 * @param being
	 * @param owners               List of people, from whom the items can be owned
	 * @param itemGroupsAmtNeeded  A mapping of group names to quantity required
	 * @param scoringMetric        The DefaultItemMetric object/subclass which calculates a score based on features
	 * 							   (See ItemMetricsUtil.java)
	 * @return
	 */
	public static Priority progressToFindItemGroups(LocalGrid grid, 
			LivingEntity being, Set<LivingEntity> owners,
			Map<String, Integer> itemGroupsAmtNeeded,
			DefaultItemMetric scoringMetric) { 
		
		//Map<Vector3i, Double> scoreMapping = new HashMap<>();
		double bestScore = -1;
		String bestGroupToFind = null;
		Vector3i bestLocation = null;
		
		for (Entry<String, Integer> entry: itemGroupsAmtNeeded.entrySet()) {
			String itemGroupName = entry.getKey();
			
			KdTree<Vector3i> nearestItemsTree = grid.getKdTreeForItemGroup(itemGroupName);
			if (nearestItemsTree != null) {
				int numCandidates = Math.min(nearestItemsTree.size(), 10);
				Collection<Vector3i> nearestCoords = nearestItemsTree.nearestNeighbourListSearch(
						numCandidates, being.location.coords);
			
				for (Vector3i itemCoords: nearestCoords) {
					LocalTile tile = grid.getTile(itemCoords);
					double score = scoringMetric.score(being, owners, grid, tile, 
							null, itemGroupsAmtNeeded);
					
					if (score < bestScore || bestGroupToFind == null) {
						bestGroupToFind = itemGroupName;
						bestScore = score;
						bestLocation = itemCoords;
					}
				}
			}
		}
		
		if (bestGroupToFind != null) {
			return new ItemGroupPickupPriority(bestLocation, bestGroupToFind, itemGroupsAmtNeeded.get(bestGroupToFind));
		}
		
		for (Entry<String, Integer> entry: itemGroupsAmtNeeded.entrySet()) {
			String itemGroupName = entry.getKey();
			int amountNeeded = entry.getValue();

			Set<Integer> groupIds = ItemData.getIdsFromNameOrGroup(itemGroupName);

			for (int groupId: groupIds) {
				List<LocalProcess> outputItemProcesses = ProcessData.getProcessesByOutput(groupId);
				for (LocalProcess process: outputItemProcesses) {
					if (process.name.startsWith("Harvest ")) {
						Set<Integer> inputTileIds = ItemData.getIdsFromNameOrGroup(process.requiredTileNameOrGroup);
						//double pickupTime = ItemData.getPickupTime(inputTileId);
						for (int inputTileId: inputTileIds) {
							Map<Integer, Double> itemDrops = process.outputItems.itemExpectation();
							double expectedNumItem = itemDrops.containsKey(inputTileId) ? 
									itemDrops.get(inputTileId) : 0;
							double effectiveNum = Math.min(amountNeeded, expectedNumItem);
							
							KdTree<Vector3i> itemTree = grid.getKdTreeForTile(inputTileId);
							if (itemTree == null || itemTree.size() == 0) continue;
							
							int numCandidates = Math.min(itemTree.size(), 10);
							Collection<Vector3i> nearestCoords = itemTree.nearestNeighbourListSearch(
									numCandidates, being.location.coords);
							for (Vector3i itemCoords: nearestCoords) {
								int distUtil = being.location.coords.manhattanDist(itemCoords);
								
								double score = distUtil - effectiveNum;
								
								if (score < bestScore || bestGroupToFind == null) {
									bestGroupToFind = itemGroupName;
									bestScore = score;
									bestLocation = itemCoords;
								}
							}
						}
					}
				}
			}
		}
		
		if (bestLocation != null) {
			return new TileHarvestPriority(bestLocation);
		}
		return new ImpossiblePriority("Could not find item groups or processes that can output harvest groups: " + itemGroupsAmtNeeded.keySet());
	}
	
	/**
	 * @return A list of vector coords representing the space. This is a list
	 * 		   because we need an indexed ordering to the target tiles 
	 */
	private static List<GridRectInterval> findBestOpenRectSpace(LocalGrid grid, Society society, 
			Set<Human> validLandOwners, Vector3i coords, Vector2i requiredSpace) {
		GridRectInterval openSpace = SpaceFillingAlg.findAvailSpaceCloseFactorClaims(grid, coords,
				requiredSpace.x, requiredSpace.y, true, society, validLandOwners, false, false, null, null);
		
		if (openSpace != null) {
			return CollectionUtil.newList(openSpace);
		}
		
		openSpace = SpaceFillingAlg.findAvailSpaceClose(grid, coords,
				requiredSpace.x, requiredSpace.y, true, society, null, false, null, null);
		if (openSpace != null) {
			return CollectionUtil.newList(openSpace);
		}
	
		return null;
	}
	
	/*
	private static Set<InventoryItem> lookForOwnedItems(Human human, Set<Integer> itemIds) {
		
	}
	
	private static Set<InventoryItem> findItemIdsToUse(Human human, Set<Integer> itemIds) {
		
	}
	*/
	
}
