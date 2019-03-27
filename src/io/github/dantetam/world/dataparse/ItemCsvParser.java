package io.github.dantetam.world.dataparse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVRecord;

import io.github.dantetam.world.dataparse.ItemTotalDrops.ItemDrop;
import io.github.dantetam.world.dataparse.ItemTotalDrops.ItemDropTrial;

public class ItemCsvParser extends WorldCsvParser {

	public static void init() {
		List<CSVRecord> csvRecords = parseCsvFile("res/items/world-itemideas.csv");
		
		Object[] stringIdData = getItemIdMappingFromCSV(csvRecords);
		Map<Integer, String> itemIdsMap = (Map<Integer, String>) stringIdData[0];
		Map<String, Integer> namesToIdsMap = (Map<String, Integer>) stringIdData[1];
		
		Map<String, List<Integer>> groupToItemsMap = getGroupsFromCSV(csvRecords);
		
		//Parse all the items line by line, keeping in mind that generic item groups may exist
		for (CSVRecord record: csvRecords) {
			String name = record.get("Item Name");
			String pattern = "(.*)(<.*>)(.*)";
		    Matcher matcher = Pattern.compile(pattern).matcher(name);
		    
		    //Save the 'base' group id as x to create id x, x+1, x+2, ...
		    String idString = record.get("Item Id");
			if (idString.isBlank()) {
				continue;
			}
			int id = Integer.parseInt(idString);
		    
			if (matcher.matches()) {
				String templateNamePre = matcher.group(0);
				String templateNamePost = matcher.group(2);
				String groupName = matcher.group(1);
				groupName = groupName.substring(1, groupName.length() - 1); //Remove the carets
				if (groupToItemsMap.containsKey(groupName)) {
					List<Integer> groupIds = groupToItemsMap.get(groupName);
					for (int offset = 0; offset < groupIds.size(); offset++) {
						int groupItemId = groupIds.get(offset);
						String groupItemName = itemIdsMap.get(groupItemId);
						String newItemName = templateNamePre + groupItemName + templateNamePost;
						Map<String, String> copyRecord = record.toMap();
						copyRecord.put("Item Name", newItemName);
						
						String newId = id + offset + "";
						copyRecord.put("Item Id", newId);
						
						processItemDataMap(copyRecord, namesToIdsMap);
					}
 				}
				else {
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
		Integer stackNum = null;
		if (!stackable.isBlank()) {
			try {
				stackNum = Integer.parseInt(stackable);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				System.err.println("Could not parse stack num in item CSV parsing");
			}
		}
		ItemTotalDrops itemDrops = processItemDropsString(record.get("OnBlockHarvest"), namesToIdsMap);
		ItemData.addItemToDatabase(id, name, placeable, groups, stackNum);
	}
	
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
				int id = namesToIdsMap.get(itemName);
				int min = args.length > 0 ? Integer.parseInt(args[1]) : 1;
				int max = args.length > 1 ? Integer.parseInt(args[2]) : 1;
				double prob = args.length > 2 ? Double.parseDouble(args[3]) : 1.0;
				trialArgs.add(new ItemDrop(id, min, max, prob));
			}
			ItemDropTrial itemDropTrial = new ItemDropTrial(trialArgs);
			itemDrops.independentDrops.add(itemDropTrial);
		}
		return itemDrops;
	}
	
	private static Object[] getItemIdMappingFromCSV(List<CSVRecord> csvRecords) {
		Map<Integer, String> itemIdsMap = new HashMap<>();
		Map<String, Integer> namesToIdsMap = new HashMap<>();
		for (CSVRecord record: parseCsvFile("res/items/world-itemideas.csv")) {
			String name = record.get("Item Name");
			String pattern = "(.*)(<.*>)(.*)";
		    Matcher matcher = Pattern.compile(pattern).matcher(name);
			if (matcher.matches()) { //No groups that contain groups, for now
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
		return new Object[] {itemIdsMap, namesToIdsMap};
	}
	
	/**
	 * <x> represents the group "x", which is a set of items that share any recipe containing the phrase <x>
	 */
	private static Map<String, List<Integer>> getGroupsFromCSV(List<CSVRecord> records) {
		Map<String, List<Integer>> groupLists = new HashMap<>();
		for (CSVRecord record: records) {
			String idString = record.get("Item Id");
			if (idString.isBlank()) {
				continue;
			}
			String groupsLine = record.get("Groups");
			if (!groupsLine.isBlank()) {
				String[] groups = groupsLine.split(";");
				int itemId = Integer.parseInt(idString);
				for (String group: groups) {
					if (!groupLists.containsKey(group)) {
						groupLists.put(group, new ArrayList<>());
					}
					groupLists.get(group).add(itemId);
				}
			}
		}
		return groupLists;
	}
	
}
