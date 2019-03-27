package io.github.dantetam.world.dataparse;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVRecord;

public class RecipeCsvParser extends WorldCsvParser {
	
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
	
	private static void processRecipeDataMap(Map<String, String> record) {
		
	}
	
}
