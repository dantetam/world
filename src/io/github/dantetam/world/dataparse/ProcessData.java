package io.github.dantetam.world.dataparse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.dantetam.world.dataparse.Process.ProcessStep;
import io.github.dantetam.world.items.InventoryItem;

public class ProcessData {

	public static List<Process> processes = new ArrayList<>();
	public static Map<Integer, List<Integer>> recipesByInput = new HashMap<>();
	public static Map<Integer, List<Integer>> recipesByOutput = new HashMap<>();

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
		for (int itemId: output.getAllItems()) {
			if (!recipesByOutput.containsKey(itemId)) {
				recipesByOutput.put(itemId, new ArrayList<>());
			}
			recipesByOutput.get(itemId).add(index);
		}
	}
	
}
