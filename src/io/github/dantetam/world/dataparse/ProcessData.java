package io.github.dantetam.world.dataparse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.dantetam.world.dataparse.Process.ProcessStep;
import io.github.dantetam.world.items.InventoryItem;

public class ProcessData {

	private static List<Process> processes = new ArrayList<>();
	private static Map<Integer, List<Integer>> recipesByInput = new HashMap<>();
	private static Map<Integer, List<Integer>> recipesByOutput = new HashMap<>();

	public static void addProcess(String name, List<InventoryItem> input, ItemTotalDrops output, 
			String[] buildingNames, boolean site, String tileFloor, List<ProcessStep> steps) {
		int index = processes.size();
		processes.add(new Process(name, input, output, buildingNames, site, tileFloor, steps));
		for (InventoryItem inputItem: input) {
			if (!recipesByInput.containsKey(inputItem.itemId)) {
				recipesByInput.put(inputItem.itemId, new ArrayList<>());
			}
			recipesByInput.get(inputItem.itemId).add(index);
		}
		if (output != null) {
			for (int itemId: output.getAllItems()) {
				if (!recipesByOutput.containsKey(itemId)) {
					recipesByOutput.put(itemId, new ArrayList<>());
				}
				recipesByOutput.get(itemId).add(index);
			}
		}
	}
	
	/**
	 * @return All processes where item with itemId is an input
	 */
	public static List<Process> getProcessesByInput(int itemId) {
		List<Process> subsetProcesses = new ArrayList<>();
		if (recipesByInput.containsKey(itemId)) {
			List<Integer> indices = recipesByInput.get(itemId);
			for (int index: indices) {
				subsetProcesses.add(processes.get(index));
			}
		}
		return subsetProcesses;
	}
	
	/**
	 * @return All processes where item with itemId is an output
	 */
	public static List<Process> getProcessesByOutput(int itemId) {
		List<Process> subsetProcesses = new ArrayList<>();
		if (recipesByOutput.containsKey(itemId)) {
			List<Integer> indices = recipesByOutput.get(itemId);
			for (int index: indices) {
				subsetProcesses.add(processes.get(index));
			}
		}
		return subsetProcesses;
	}
	
}
