package io.github.dantetam.world.dataparse;

import java.util.List;

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
	public String[] requiredBuildNameOrGroups;
	public boolean isCreatedAtSite;
	public String requiredTileNameOrGroup;
	public List<ProcessStep> processSteps;
	
	public Process(String name, List<InventoryItem> input, ItemTotalDrops output, 
			String[] buildingNames, boolean site, String tileFloorId, List<ProcessStep> steps) {
		this.name = name;
		this.inputItems = input;
		this.outputItems = output;
		this.requiredBuildNameOrGroups = buildingNames;
		this.isCreatedAtSite = site;
		this.requiredTileNameOrGroup = tileFloorId;
		this.processSteps = steps;
	}
	
	public static class ProcessStep {
		public String stepType;
		public int timeTicks;
		
		public ProcessStep(String type, int time) {
			this.stepType = type;
			this.timeTicks = time;
		}
	}
	
}
