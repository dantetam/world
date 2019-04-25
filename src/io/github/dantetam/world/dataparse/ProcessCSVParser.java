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
		String pattern = "\\<(.*?)\\>";
		String[] multipleInput = allInput.split("/");
		
		String requiredBuilding = record.get("Required Buildings");
	    Matcher buildingMatcher = Pattern.compile(pattern).matcher(requiredBuilding);
		
	    String requiredLocation = record.get("Required Location");
	    Matcher tileMatcher = Pattern.compile(pattern).matcher(requiredLocation);
	    
		for (String singleInput : multipleInput) {
		    Matcher inputGroupMatcher = Pattern.compile(pattern).matcher(singleInput);
		    
		    //System.out.println(singleInput + " o " + inputGroupMatcher.find());
		    
			//Duplicate a whole group into its item rows if the group <caret> notation is present
		    //Continue the recursion because of more data to process
			if (inputGroupMatcher.find()) {
				String groupName = inputGroupMatcher.group(1);
				//groupName = groupName.substring(1, groupName.length() - 1); //Remove the carets
				
				List<String> groupNames = ItemCSVParser.groupSyntaxShortcuts.get(groupName);
				if (groupNames == null) {
					System.err.println("Could not find group name: " + groupName);
				}
				else {
					for (String groupItemName: groupNames) {
						String newItemName = singleInput.replaceFirst("\\<(.*?)\\>", groupItemName);
						
						Map<String, String> copyRecord = new HashMap<>(record);
						
						copyRecord.put("Required Input", newItemName);
						
						String outputData = copyRecord.get("Output");
						Matcher outputGroupMatcher = Pattern.compile(pattern).matcher(outputData);
						if (outputGroupMatcher.find()) {
							String newOutputString = outputData.replaceFirst("\\<(.*?)\\>", groupItemName);
							copyRecord.put("Output", newOutputString);
						}
						
						outputData = copyRecord.get("Output");
						//TODO: Generalize for all functions
						String functionPattern = "(.*)(refine\\((.*)\\))(.*)";
					    Matcher outputFuncMatcher = Pattern.compile(functionPattern).matcher(outputData);
						if (outputFuncMatcher.find()) {
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
			}
			else if (buildingMatcher.find()) {
				String groupName = buildingMatcher.group(1);
				List<String> groupNames = ItemCSVParser.groupSyntaxShortcuts.get(groupName);
				if (groupNames == null) {
					System.err.println("Could not find group name: " + groupName);
				}
				else {
					for (String groupItemName: groupNames) {
						Map<String, String> copyRecord = new HashMap<>(record);
						String newBuildingName = requiredBuilding.replaceFirst("\\<(.*?)\\>", groupItemName);
						copyRecord.put("Required Buildings", newBuildingName);
						preprocessExpandingRecipe(copyRecord);
					}
				}
			}
			else if (tileMatcher.find()) {
				String groupName = tileMatcher.group(1);
				List<String> groupNames = ItemCSVParser.groupSyntaxShortcuts.get(groupName);
				if (groupNames == null) {
					System.err.println("Could not find group name: " + groupName);
				}
				else {
					for (String groupItemName: groupNames) {
						Map<String, String> copyRecord = new HashMap<>(record);
						String newBuildingName = requiredLocation.replaceFirst("\\<(.*?)\\>", groupItemName);
						copyRecord.put("Required Location", newBuildingName);
						preprocessExpandingRecipe(copyRecord);
					}
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
				tileStr.strip() : null;
				
		List<ProcessStep> steps = getProcessingSteps(record.get("Process Steps"));
		
		String processName = record.get("Process Name");
		if (processName.isBlank()) {
			processName = outputString;
		}
		
		List<ProcessStep> processResActions = null;
		String processResActionsStr = record.get("Result Action");
		if (!processResActionsStr.isBlank()) {
			processResActions = getProcessingSteps(record.get("Result Action"));
		}
		
		ProcessData.addProcess(processName, inputItems, processOutput, 
				buildingNamesString, site, tileFloorId, steps, processResActions);
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
