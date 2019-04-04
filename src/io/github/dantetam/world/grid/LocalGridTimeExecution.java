package io.github.dantetam.world.grid;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civilization.Human;
import io.github.dantetam.world.civilization.LivingEntity;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.process.Process;
import io.github.dantetam.world.process.Process.ProcessStep;
import io.github.dantetam.world.process.priority.Priority;
import io.github.dantetam.world.process.prioritytask.DropoffInvTask;
import io.github.dantetam.world.process.prioritytask.MoveTask;
import io.github.dantetam.world.process.prioritytask.PickupTask;
import io.github.dantetam.world.process.prioritytask.Task;

public class LocalGridTimeExecution {
	
	public static double numDayTicks = 0;
	
	public static void tick(LocalGrid grid, Society society) {
		if (numDayTicks % 1440 == 0) {
			numDayTicks = 0;
			assignAllHumansJobs(society);
		}
		for (Human human: society.getAllPeople()) {
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
			else if (human.activePriority != null) {
				human.currentQueueTasks = getTasksFromPriority(human, human.activePriority);
			}
			else if (human.processProgress != null) {
				ProcessStep step = human.processProgress.processSteps.get(0);
				human.activePriority = getPriorityForStep(human, step);
			}
		}
		numDayTicks++;
	}
	
	private static List<Task> getTasksFromPriority(LivingEntity being, Priority priority) {
		
	}
	
	private static Priority getPriorityForStep(LivingEntity being, ProcessStep step) {
		
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
