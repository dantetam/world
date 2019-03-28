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
					String templateNamePre = inputGroupMatcher.group(1);
					String templateNamePost = inputGroupMatcher.group(3);
					String groupName = inputGroupMatcher.group(2);
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
							String outputNamePre = outputGroupMatcher.group(1);
							String outputNamePost = outputGroupMatcher.group(3);
							String newOutputString = outputNamePre + groupItemName + outputNamePost;
							copyRecord.put("Output", newOutputString);
						}
						
						outputData = copyRecord.get("Output");
						//TODO: Generalize for all functions
						String functionPattern = "(.*)(refine\\((.*)\\))(.*)";
					    Matcher outputFuncMatcher = Pattern.compile(functionPattern).matcher(outputData);
						if (outputFuncMatcher.matches()) {
							String rawForm = outputFuncMatcher.group(3);
							System.out.println(rawForm);
							int refinedForm = ItemData.getRefinedFormId(ItemData.getIdFromName(rawForm));
							String refinedFormName = ItemData.getNameFromId(refinedForm);
							
							String outputNamePre = outputFuncMatcher.group(1),
									outputNamePost = outputFuncMatcher.group(4);
							
							String newOutputString = outputNamePre + refinedFormName + outputNamePost;
							copyRecord.put("Output", newOutputString);
						}
						
						System.out.println(copyRecord);
						
						processRecipeDataMap(copyRecord);
					}
					
				}
				else {
					processRecipeDataMap(record.toMap());
				}
			}
		}
	}
	
	private static void processRecipeDataMap(Map<String, String> record) {
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
		
		String outputString = record.get("Output");
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
		
		String processName = record.get("Process Name");
		if (processName.isBlank()) {
			processName = outputString;
		}
		
		ProcessData.addProcess(processName, inputItems, processOutput, 
				buildingIds, site, tileFloorId, steps);
	}
	
	private static List<ProcessStep> getProcessingSteps(String processString) {
		List<ProcessStep> steps = new ArrayList<>();
		String[] originalSteps = processString.split("/");
		for (String originalStep : originalSteps) {
			String[] originalStepArgs = originalStep.split(",");
			int time = originalStepArgs.length > 1 ? Integer.parseInt(originalStepArgs[1].trim()) : 0;
			ProcessStep step = new ProcessStep(originalStepArgs[0], time);
			steps.add(step);
		}
		return steps;
	}
	
}
