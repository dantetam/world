package io.github.dantetam.world.dataparse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVRecord;

import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.dataparse.ItemTotalDrops.ItemDrop;
import io.github.dantetam.world.dataparse.ItemTotalDrops.ItemDropTrial;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.LocalProcess.ProcessStep;

public class ItemCSVParser extends WorldCsvParser {

	public static Map<String, List<String>> groupSyntaxShortcuts;
	
	public static void init() {
		List<CSVRecord> csvRecords = parseCsvFile("res/items/world-itemideas.csv");
		
		Map[] stringIdData = getItemIdMappingFromCSV(csvRecords);
		Map<Integer, String> itemIdsMap = (Map<Integer, String>) stringIdData[0];
		Map<String, Integer> namesToIdsMap = (Map<String, Integer>) stringIdData[1];
		
		groupSyntaxShortcuts = getGroupsFromCSV(csvRecords);
		
		//Parse all the items line by line, keeping in mind that generic item groups may exist
		for (CSVRecord record: csvRecords) {
			String name = record.get("Item Name");
			String pattern = "\\<(.*?)\\>";
		    Matcher matcher = Pattern.compile(pattern).matcher(name);
		    
		    //Save the 'base' group id as x to create id x, x+1, x+2, ...
		    String idString = record.get("Item Id");
			if (idString.isBlank()) {
				continue;
			}
			int id = Integer.parseInt(idString);
		    
			//Duplicate a whole group into its item rows if the group <caret> notation is present
			if (matcher.find()) {
				String groupName = matcher.group(1);
				//groupName = groupName.substring(1, groupName.length() - 1); //Remove the carets
				if (groupSyntaxShortcuts.containsKey(groupName)) {
					List<String> groupItemNames = groupSyntaxShortcuts.get(groupName);
					for (int offset = 0; offset < groupItemNames.size(); offset++) {
						String groupItemName = groupItemNames.get(offset);
						String newItemName = name.replaceFirst("\\<(.*?)\\>", groupItemName);
						Map<String, String> copyRecord = record.toMap();
						copyRecord.put("Item Name", newItemName);
						
						String newId = id + offset + "";
						copyRecord.put("Item Id", newId);
						
						processItemDataMap(copyRecord, namesToIdsMap);
					}
 				}
				else {
					CustomLog.errPrintln(record.toMap());
					throw new IllegalArgumentException("While parsing item csv, could not find group name: " + groupName);
				}
			}
			else {
				processItemDataMap(record.toMap(), namesToIdsMap);
			}
		} 
	}
	
	private static void processItemDataMap(Map<String, String> record, Map<String, Integer> namesToIdsMap) {
		String idString = record.get("Item Id");
		if (idString.isBlank()) {
			return;
		}
		int id = Integer.parseInt(idString);
		String name = record.get("Item Name");
		boolean placeable = record.get("CanBeBlock").equals("Y");
		String[] groups = record.get("Groups").split(";");
		String stackable = record.get("Stackable");
		Integer stackNum = new Integer(1);
		if (!stackable.isBlank()) {
			try {
				stackNum = Integer.parseInt(stackable);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				CustomLog.errPrintln("Could not parse stack num in item CSV parsing");
			}
		}
		ItemTotalDrops itemDrops = processItemDropsString(record.get("OnBlockHarvest"), namesToIdsMap);
		String refinedFormName = record.get("Refined Form").strip();
		int refinedId = ItemData.ITEM_EMPTY_ID;
		
		String pickupTimeStr = record.get("PickupTime");
		int pickupTime = pickupTimeStr.isBlank() ? 30 : Integer.parseInt(pickupTimeStr);
		
		String baseValueStr = record.get("Base Value");
		double baseValue = baseValueStr.isBlank() ? 0 : Double.parseDouble(baseValueStr);
		
		String beautyValueStr = record.get("Beauty");
		double beautyValue = beautyValueStr.isBlank() ? 1.0 : Double.parseDouble(beautyValueStr);
		
		String processString = record.get("Action");
		List<ProcessStep> itemActions = null;
		if (!processString.isBlank()) {
			itemActions = ProcessCSVParser.getProcessingSteps(processString);
				
			if (itemActions.size() > 0) {
				List<InventoryItem> singleItem = new ArrayList<InventoryItem>() {{
					add(new InventoryItem(id, 1, name));
				}};
				
				List<ProcessStep> steps = new ArrayList<>();
				steps.add(new ProcessStep("Wait", pickupTime));
				ProcessData.addProcess("Consume Item " + name, singleItem, null, null, false, 
						null, steps, itemActions, 1);
			}
		}
		
		String propertyString = record.get("Property");
		List<ProcessStep> propertyProcessStep = null;
		if (!propertyString.isBlank()) {
			propertyProcessStep = ProcessCSVParser.getProcessingSteps(propertyString);
		}
		
		if (!refinedFormName.isBlank()) {
			if (namesToIdsMap.containsKey(refinedFormName))
				refinedId = namesToIdsMap.get(refinedFormName);
			else
				throw new IllegalArgumentException("Could not find refined form of name: " + refinedFormName);
		}
		
		List<Vector3i> specBuildOffsets = new ArrayList<>();
		String offsetsString = record.get("Building Bounds");
		if (!offsetsString.isBlank()) {
			String[] vecStrings = offsetsString.split("/");
			for (String vecString: vecStrings) {
				String[] vecData = vecString.split(",");
				if (vecData.length != 3) throw new IllegalArgumentException("Could not parse Vector3i: " + vecString);
				try {
					int x = Integer.parseInt(vecData[0]), y = Integer.parseInt(vecData[1]), z = Integer.parseInt(vecData[2]);
					specBuildOffsets.add(new Vector3i(x,y,z));
				} catch (NumberFormatException e) {
					e.printStackTrace();
					throw new IllegalArgumentException("Could not parse Vector3i: " + vecString);
				}
			}
		}
		
		ItemData.addItemToDatabase(id, name, placeable, groups, stackNum, refinedId, itemDrops, 
				pickupTime, baseValue, beautyValue, itemActions, propertyProcessStep, 
				specBuildOffsets);
		
		//Create a new process for the item harvesting for resource utility purposes
		if (placeable) {
			if (record.get("Groups").contains("Building")) {
				List<ProcessStep> steps = new ArrayList<>();
				steps.add(new ProcessStep("HBuilding", pickupTime));
				steps.add(new ProcessStep("O", 0));
				ProcessData.addProcess("Harvest Building " + name, new ArrayList<>(), itemDrops, name, false, 
						null, steps, null, stackNum);
			}
			else {
				List<ProcessStep> steps = new ArrayList<>();
				steps.add(new ProcessStep("HTile", pickupTime));
				steps.add(new ProcessStep("O", 0));
				ProcessData.addProcess("Harvest Tile " + name, new ArrayList<>(), itemDrops, null, false, 
						name, steps, null, stackNum);
			}
		}
	}
	
	/**
	 * 
	 * @param dropString
	 * @param namesToIdsMap
	 * @return Convert a string from the CSV into the ItemTotalDrops object, a code representation
	 * 	of a probabilistic item dropping.
	 */
	private static ItemTotalDrops processItemDropsString(String dropString, Map<String, Integer> namesToIdsMap) {
		if (dropString.isBlank()) return null;
		String[] trials = dropString.split("/");
		ItemTotalDrops itemDrops = new ItemTotalDrops();
		for (String trial : trials) {
			List<ItemDrop> trialArgs = new ArrayList<>();
			String[] itemDropsStr = trial.split(";");
			for (String itemDrop : itemDropsStr) {
				String[] args = itemDrop.split(",");
				String itemName = args[0].strip();
				int id = 0;
				try {
					id = namesToIdsMap.get(itemName);
				} catch (NullPointerException e) {
					//e.printStackTrace();
					CustomLog.errPrintln("Could not find item name: " + itemName);
					id = ItemData.generateItem(itemName);
				}
				
				try {
					int min = args.length >= 2 ? Integer.parseInt(args[1].strip()) : 1;
					int max = args.length >= 3 ? Integer.parseInt(args[2].strip()) : 1;
					double prob = args.length >= 4 ? Double.parseDouble(args[3].strip()) : 1.0;
					trialArgs.add(new ItemDrop(id, min, max, prob));
				} catch (NumberFormatException e) {
					e.printStackTrace();
					throw new IllegalArgumentException("Drops must be in the form: "
							+ "String name, (int min, int max, double prob), given drop: \n" 
							+ dropString);
				}
			}
			ItemDropTrial itemDropTrial = new ItemDropTrial(trialArgs);
			itemDrops.addItemDropTrial(itemDropTrial);
		}
		return itemDrops;
	}
	public static ItemTotalDrops processItemDropsString(String dropString) {
		return processItemDropsString(dropString, ItemData.itemNamesToIds);
	}
	
	/**
	 * @return Return the map between item id and item name (a one-to-one mapping), 
	 * in two maps that are the inverse of each other.
	 */
	private static Map[] getItemIdMappingFromCSV(List<CSVRecord> csvRecords) {
		Map<Integer, String> itemIdsMap = new HashMap<>();
		Map<String, Integer> namesToIdsMap = new HashMap<>();
		for (CSVRecord record: parseCsvFile("res/items/world-itemideas.csv")) {
			String name = record.get("Item Name");
			String pattern = "(.*)(<.*>)(.*)";
		    Matcher matcher = Pattern.compile(pattern).matcher(name);
			if (matcher.find()) { //No groups that contain groups, for now
				continue;
			}
			String idString = record.get("Item Id");
			if (idString.isBlank()) {
				continue;
			}
			int id = Integer.parseInt(idString);
			itemIdsMap.put(id, name);
			namesToIdsMap.put(name, id);
		}
		return new Map[] {itemIdsMap, namesToIdsMap};
	}
	
	/**
	 * <x> represents the group "x", which is a set of items that share any recipe containing the phrase <x>
	 */
	private static Map<String, List<String>> getGroupsFromCSV(List<CSVRecord> records) {
		Map<String, List<String>> groupLists = new HashMap<>();
		for (CSVRecord record: records) {
			String itemName = record.get("Item Name");
			String groupsLine = record.get("Groups");
			if (!groupsLine.isBlank()) {
				String[] groups = groupsLine.split(";");
				for (String group: groups) {
					if (!groupLists.containsKey(group)) {
						groupLists.put(group, new ArrayList<>());
					}
					groupLists.get(group).add(itemName);
				}
			}
		}
		return groupLists;
	}
	
}
