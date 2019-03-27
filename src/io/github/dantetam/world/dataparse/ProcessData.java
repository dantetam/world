package io.github.dantetam.world.dataparse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.dantetam.world.dataparse.Process.ProcessStep;
import io.github.dantetam.world.grid.InventoryItem;

public class ProcessData {

	public List<Process> processes = new ArrayList<>();
	public Map<Integer, List<Integer>> recipesByInput = new HashMap<>();
	public Map<Integer, List<Integer>> recipesByOutput = new HashMap<>();

	public void addProcess(String name, List<InventoryItem> input, List<InventoryItem> output, 
			List<Integer> buildingIds, boolean site, int tileFloorId, List<ProcessStep> steps) {
		int index = processes.size();
		processes.add(new Process(name, input, output, buildingIds, site, tileFloorId, steps));
		for (InventoryItem inputItem: input) {
			if (!recipesByInput.containsKey(inputItem.itemId)) {
				recipesByInput.put(inputItem.itemId, new ArrayList<>());
			}
			recipesByInput.get(inputItem.itemId).add(index);
		}
		for (InventoryItem outputItem: output) {
			if (!recipesByOutput.containsKey(outputItem.itemId)) {
				recipesByOutput.put(outputItem.itemId, new ArrayList<>());
			}
			recipesByOutput.get(outputItem.itemId).add(index);
		}
	}
	
}
