package io.github.dantetam.world.dataparse;

import java.util.List;

import io.github.dantetam.world.grid.InventoryItem;

/**
 * A process or flow which a person can take up and complete.
 * Flows have predefined steps that are advanced through an action pseudocode.
 * See the file "res/world-recipes.csv"
 * @author Dante
 *
 */

public class Process {

	public String name;
	public List<InventoryItem> inputItems, outputItems;
	public List<Integer> requiredBuildingIds;
	public boolean isCreatedAtSite;
	public int requiredTileFloorId;
	public List<ProcessStep> processSteps;
	
	public Process(String name, List<InventoryItem> input, List<InventoryItem> output, 
			List<Integer> buildingIds, boolean site, int tileFloorId, List<ProcessStep> steps) {
		this.name = name;
		this.inputItems = input;
		this.outputItems = output;
		this.requiredBuildingIds = buildingIds;
		this.isCreatedAtSite = site;
		this.requiredTileFloorId = tileFloorId;
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
