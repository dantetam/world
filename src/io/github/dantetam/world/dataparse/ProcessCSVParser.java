package io.github.dantetam.world.dataparse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;

import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.Process.ProcessStep;

public class ProcessCSVParser extends WorldCsvParser {
	
	public static void init() {
		List<CSVRecord> csvRecords = parseCsvFile("res/items/world-recipes.csv");
		for (CSVRecord csvRecord : csvRecords) {
			Map<String, String> record = csvRecord.toMap();
			preprocessExpandingRecipe(record);
		}
	}
	
	private static void preprocessExpandingRecipe(Map<String, String> record) {
		String allInput = record.get("Required Input");
		
		String[] multipleInput = allInput.split("/");
		
		for (String singleInput : multipleInput) {
			String pattern = "(.*)(<.*>)(.*)";
		    Matcher inputGroupMatcher = Pattern.compile(pattern).matcher(singleInput);
		    
			//Duplicate a whole group into its item rows if the group <caret> notation is present
		    //Continue the recursion because of more data to process
			if (inputGroupMatcher.matches()) {
				String templateNamePre = inputGroupMatcher.group(1);
				String templateNamePost = inputGroupMatcher.group(3);
				String groupName = inputGroupMatcher.group(2);
				groupName = groupName.substring(1, groupName.length() - 1); //Remove the carets
				
				Set<Integer> groupIds = ItemData.getGroupIds(groupName);
				
				for (Integer groupItemId : groupIds) {
					String groupItemName = ItemData.getNameFromId(groupItemId);
					String newItemName = templateNamePre + groupItemName + templateNamePost;
					
					Map<String, String> copyRecord = new HashMap<>(record);
					
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
						int refinedForm = ItemData.getRefinedFormId(ItemData.getIdFromName(rawForm));
						String refinedFormName = ItemData.getNameFromId(refinedForm);
						
						String outputNamePre = outputFuncMatcher.group(1),
								outputNamePost = outputFuncMatcher.group(4);
						
						String newOutputString = outputNamePre + refinedFormName + outputNamePost;
						copyRecord.put("Output", newOutputString);
					}
					
					preprocessExpandingRecipe(copyRecord);
				}
				
			}
			else { //No more groups to expand within this recipe, actually begin to process a 'pure' recipe
				Map<String, String> copyRecord = new HashMap<>(record);
				copyRecord.put("Required Input", singleInput);
				processRecipeDataMap(copyRecord);
			}
		}
	}
	
	private static void processRecipeDataMap(Map<String, String> record) {
		List<InventoryItem> inputItems = new ArrayList<>();
		String inputString = record.get("Required Input");
		if (!inputString.isBlank()) {
			String[] itemStrings = inputString.split(",");
			for (String itemString : itemStrings) {
				itemString = itemString.strip();
				int index = itemString.indexOf(" ");
				int quantity = Integer.parseInt(itemString.substring(0, index));
				String itemName = itemString.substring(index + 1).strip();
				int id = ItemData.getIdFromName(itemName);
				inputItems.add(new InventoryItem(id, quantity, ""));
			}
		}
		
		String outputString = record.get("Output");
		ItemTotalDrops processOutput = ItemCSVParser.processItemDropsString(outputString);
		
		String buildingNamesString = record.get("Required Buildings");
		if (buildingNamesString.isBlank()) buildingNamesString = null;
		
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
				buildingNamesString, site, tileFloorId, steps);
	}
	
	static List<ProcessStep> getProcessingSteps(String processString) {
		List<ProcessStep> steps = new ArrayList<>();
		String[] originalSteps = processString.split("/");
		for (String originalStep : originalSteps) {
			String[] originalStepArgs = originalStep.split(",");
			originalStepArgs[0] = originalStepArgs[0].strip();
			String timeString = "1";
			if (originalStepArgs.length >= 2) {
				timeString = originalStepArgs[1].trim();
				if (timeString.contains("U(")) {
					timeString = timeString.replaceAll("[^\\d.]", "");
				}
			}
			ProcessStep step;
			int time = originalStepArgs.length > 1 ? Integer.parseInt(timeString) : 0;
			if (originalStepArgs.length >= 3 && originalStepArgs[2].indexOf(")") == -1) {
				double modifierData = Double.parseDouble(originalStepArgs[2]);
				step = new ProcessStep(originalStepArgs[0], time, modifierData);
			}
			else {
				step = new ProcessStep(originalStepArgs[0], time);
			}
			steps.add(step);
		}
		return steps;
	}
	
}
