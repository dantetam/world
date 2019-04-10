package io.github.dantetam.world.process;

import java.util.ArrayList;
import java.util.List;

import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ItemTotalDrops;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;

/**
 * A process or flow which a person can take up and complete.
 * Flows have predefined steps that are advanced through an action pseudocode.
 * See the file "res/world-recipes.csv"
 * @author Dante
 *
 */

public class Process {

	public String name;
	public List<InventoryItem> inputItems;
	public ItemTotalDrops outputItems;
	public String requiredBuildNameOrGroup;
	public boolean isCreatedAtSite;
	public String requiredTileNameOrGroup;
	public List<ProcessStep> processSteps;
	
	public Process(String name, List<InventoryItem> input, ItemTotalDrops output, 
			String buildingName, boolean site, String tileFloorId, List<ProcessStep> steps) {
		this.name = name;
		this.inputItems = input;
		this.outputItems = output;
		this.requiredBuildNameOrGroup = buildingName;
		this.isCreatedAtSite = site;
		this.requiredTileNameOrGroup = tileFloorId;
		this.processSteps = steps;
	}
	
	public ProcessStep findStepName(String stepType) {
		for (ProcessStep step: processSteps) {
			if (step.stepType.equals(stepType)) {
				return step;
			}
		}
		return null;
	}
	
	public int totalTime() {
		int sum = 0;
		for (ProcessStep step: processSteps) {
			sum += step.timeTicks;
		}
		return sum;
	}
	
	public String toString() {
		String result = "Process: " + name + "/ Input: ";
		
		result += new Inventory(inputItems).toUniqueItemsMap();
		
		result += "/ Output: ";
		for (int id: outputItems.getAllItems()) {
			result += ItemData.getNameFromId(id) + "; ";
		}
		result += "/ Process: ";
		for (ProcessStep step: processSteps) {
			result += step.toString() + "; ";
		}
		return result;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof Process)) return false;
		return ((Process) other).name.equals(this.name);
	}
	
	public int hashCode() {
		return name.hashCode();
	}
	
	public Process clone() {
		List<ProcessStep> steps = new ArrayList<>();
		for (ProcessStep step: this.processSteps) {
			steps.add(new ProcessStep(step.stepType, step.timeTicks, step.modifier));
		}
		Process clone = new Process(this.name, this.inputItems, this.outputItems, 
				this.requiredBuildNameOrGroup, this.isCreatedAtSite, this.requiredTileNameOrGroup,
				steps);
		return clone;
	}
	
	public static class ProcessStep {
		public String stepType;
		public int timeTicks;
		public double modifier;
		
		public ProcessStep(String type, int time) {
			this.stepType = type;
			this.timeTicks = time;
		}
		
		public ProcessStep(String type, int time, double modifier) {
			this.stepType = type;
			this.timeTicks = time;
			this.modifier = modifier;
		}
		
		public String toString() {
			return stepType + ", " + timeTicks + ", " + modifier;
		}
	}
	
}
