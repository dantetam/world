package io.github.dantetam.world.dataparse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;

import io.github.dantetam.world.dataparse.Process.ProcessStep;
import io.github.dantetam.world.grid.InventoryItem;

public class ProcessCSVParser extends WorldCsvParser {
	
	public static void init() {
		List<CSVRecord> csvRecords = parseCsvFile("res/items/world-recipes.csv");
		for (CSVRecord record : csvRecords) {
			String allInput = record.get("Required Input");
			
			String[] multipleInput = allInput.split("/");
			
			for (String singleInput : multipleInput) {
				String pattern = "(.*)(<.*>)(.*)";
			    Matcher inputGroupMatcher = Pattern.compile(pattern).matcher(singleInput);
			    
				//Duplicate a whole group into its item rows if the group <caret> notation is present
				if (inputGroupMatcher.matches()) {
					String templateNamePre = inputGroupMatcher.group(0);
					String templateNamePost = inputGroupMatcher.group(2);
					String groupName = inputGroupMatcher.group(1);
					groupName = groupName.substring(1, groupName.length() - 1); //Remove the carets
					
					List<Integer> groupIds = ItemData.getGroupIds(groupName);
					
					for (int offset = 0; offset < groupIds.size(); offset++) {
						int groupItemId = groupIds.get(offset);
						String groupItemName = ItemData.getNameFromId(groupItemId);
						String newItemName = templateNamePre + groupItemName + templateNamePost;
						
						Map<String, String> copyRecord = record.toMap();
						
						copyRecord.put("Required Input", newItemName);
						
						String outputData = copyRecord.get("Output");
						Matcher outputGroupMatcher = Pattern.compile(pattern).matcher(outputData);
						if (outputGroupMatcher.matches()) {
							String outputNamePre = outputGroupMatcher.group(0);
							String outputNamePost = outputGroupMatcher.group(2);
							String newOutputString = outputNamePre + groupItemName + outputNamePost;
							copyRecord.put("Output", newOutputString);
						}
						
						processRecipeDataMap(copyRecord);
					}
					
				}
				else {
					processRecipeDataMap(record.toMap());
				}
			}
		}
	}
	
	public Process(String name, List<InventoryItem> input, List<InventoryItem> output, 
			List<Integer> buildingIds, boolean site, int tileFloorId, List<ProcessStep> steps)
	
	private static void processRecipeDataMap(Map<String, String> record) {
		String processName = record.get("Process Name");
		
		List<InventoryItem> inputItems = new ArrayList<>();
		String inputString = record.get("Required Input");
		String[] itemStrings = inputString.split(",");
		for (String itemString : itemStrings) {
			int index = itemString.indexOf(" ");
			int quantity = Integer.parseInt(itemString.substring(0, index));
			String itemName = itemString.substring(index + 1).strip();
			int id = ItemData.getIdFromName(itemName);
			inputItems.add(new InventoryItem(id, quantity, ""));
		}
		
		String outputString = record.get("Required Output");
		ItemTotalDrops processOutput = ItemCSVParser.processItemDropsString(outputString);
		
		List<Integer> buildingIds = null;
		String buildingNamesString = record.get("Required Buildings");
		if (!buildingNamesString.isBlank()) {
			String[] buildingNames = buildingNamesString.split("/");
			buildingIds = Arrays.stream(buildingNames)
					.map(name -> ItemData.getIdFromName(name))
					.collect(Collectors.toList());
		}
		
		boolean site = record.get("Is Site").equals("Y");
		
		String tileStr = record.get("Required Location");
		String tileFloorId = !tileStr.isBlank() ? 
				tileStr.strip().substring(1, tileStr.length() - 1) : null;
				
		List<ProcessStep> steps = getProcessingSteps(record.get("Process Steps"));
		
		if (processName.isBlank()) {
			processName = outputString;
		}
	}
	
	private static List<ProcessStep> getProcessingSteps(String processString) {
		String[] originalSteps = processString.split("/");
	}
	
	public ProcessStep(String type, int time) {
		this.stepType = type;
		this.timeTicks = time;
	}
	
}
