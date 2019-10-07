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

import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.world.civilization.SkillProcessDistribution;
import io.github.dantetam.world.civilization.SkillProcessDistribution.BasicFunction;
import io.github.dantetam.world.civilization.SkillProcessDistribution.NoiseDistribution;
import io.github.dantetam.world.civilization.SkillProcessDistribution.NoiseDistributionType;
import io.github.dantetam.world.civilization.SkillProcessDistribution.ProcessModKey;
import io.github.dantetam.world.civilization.SkillProcessDistribution.SkillOutFunction;
import io.github.dantetam.world.civilization.SkillProcessDistribution.SkillProcessMod;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.LocalProcess.ProcessStep;

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
		    
		    //CustomLog.outPrintln(singleInput + " o " + inputGroupMatcher.find());
		    
			//Duplicate a whole group into its item rows if the group <caret> notation is present
		    //Continue the recursion because of more data to process
			if (inputGroupMatcher.find()) {
				String groupName = inputGroupMatcher.group(1);
				//groupName = groupName.substring(1, groupName.length() - 1); //Remove the carets
				
				List<String> groupNames = ItemCSVParser.groupSyntaxShortcuts.get(groupName);
				if (groupNames == null) {
					CustomLog.errPrintln("(ProcessCSVParser) Could not find group name: " + groupName);
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
						//TODO: Generalize for all functions parsed in CSV
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
					CustomLog.errPrintln("Could not find group name: " + groupName);
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
					CustomLog.errPrintln("Could not find group name: " + groupName);
				}
				else {
					/*
					 * No group overriding/preprocessing
					 * Use the group name moving forward to keep the recipe together
					 */
					
					/*
					for (String groupItemName: groupNames) {
						Map<String, String> copyRecord = new HashMap<>(record);
						String newBuildingName = requiredLocation.replaceFirst("\\<(.*?)\\>", groupItemName);
						copyRecord.put("Required Location", newBuildingName);
						preprocessExpandingRecipe(copyRecord);
					}
					*/
					
					Map<String, String> copyRecord = new HashMap<>(record);
					copyRecord.put("Required Location", groupName);
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
				tileStr.strip() : null;
				
		List<ProcessStep> steps = getProcessingSteps(record.get("Process Steps"));
		
		String processName = record.get("Process Name");
		if (processName.isBlank()) {
			processName = outputString;
		}
		
		List<ProcessStep> processResActions = new ArrayList<>();
		String processResActionsStr = record.get("Result Action");
		if (!processResActionsStr.isBlank()) {
			processResActions = getProcessingSteps(record.get("Result Action"));
		}
		
		String recRepeatStr = record.get("RecRepeats");
		int recRepeat = 1;
		if (!recRepeatStr.isBlank()) {
			recRepeat = Integer.parseInt(recRepeatStr);
		}
		
		String skillProcString = record.get("Skill Process Distribution");
		SkillProcessDistribution skillProcDistr = null;
		if (!skillProcString.isBlank()) {
			skillProcDistr = parseSkillProcDistr(skillProcString);
		}
		else {
			//Generate a default skill process distribution based on the skills used in the process,
			//if any skills were used.
			List<String> skillNames = new ArrayList<>();
			for (ProcessStep step: steps) {
				if (step.stepType.startsWith("S") || step.stepType.startsWith("U")) {
					String skillName = step.stepType.substring(1);
					skillNames.add(skillName);
				}
			}
			
			if (skillNames.size() != 0) { 	
				//Separated loops, in case of a multivariate function later on that requires all skill names at once
				String inputModsString = "";
				for (String skillName: skillNames) {
					inputModsString += skillName + ",outQuality,1,0.3,LINEAR," + (1.7 / 20) + ";";
					inputModsString += skillName + ",outQuantMulti,1,0.75,LINEAR," + (2.25 / 20) + ";";
					inputModsString += skillName + ",timeSupMul,1,1,LINEAR," + (-0.5 / 20) + ";";
				}
				skillProcDistr = parseSkillProcDistr(inputModsString);
			}
		}
		
		ProcessData.addProcess(processName, inputItems, processOutput, 
				buildingNamesString, site, tileFloorId, steps, processResActions, recRepeat, skillProcDistr);
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
	
	/**
	 * See SkillProcessDistribution.java for a complete explanation on creating this object.
	 */
	static SkillProcessDistribution parseSkillProcDistr(String string) {
		if (string.isBlank()) return null;
		SkillProcessDistribution distr = new SkillProcessDistribution();
		String[] mods = string.split(";");
		for (String mod: mods) {
			if (mod.isBlank()) continue;
			String[] tokens = mod.split(",");
			if (tokens.length != 6 && tokens.length != 9) {
				throw new IllegalArgumentException("Skill Proc Distribution Parsing takes 6 or 9 args, "
						+ "see documentation in SkillProcessDistribution.java. Given input: " + mod);
			}
			
			try {
				SkillOutFunction skillFunc = new SkillOutFunction(
						Double.parseDouble(tokens[2]), Double.parseDouble(tokens[3]), 
						BasicFunction.valueOf(tokens[4]), Double.parseDouble(tokens[5])
						);
				SkillProcessMod skillProcMod = new SkillProcessMod(tokens[0].strip(), 
						ProcessModKey.valueOf(tokens[1].strip()), skillFunc);
				if (tokens.length == 9) {
					skillProcMod.noise = new NoiseDistribution(
							NoiseDistributionType.valueOf(tokens[6].strip()), 
							Double.parseDouble(tokens[7]), Double.parseDouble(tokens[8])
							);
				}
				distr.skillProcessMods.add(skillProcMod);
			} catch (NumberFormatException e2) {
				e2.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} 
		}
		
		return distr;
	}
	
}
