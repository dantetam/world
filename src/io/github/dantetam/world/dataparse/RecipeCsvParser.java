package io.github.dantetam.world.dataparse;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVRecord;

public class RecipeCsvParser extends WorldCsvParser {

	TODO
	
	public static void init() {
		List<CSVRecord> csvRecords = parseCsvFile("res/items/world-recipes.csv");
		for (CSVRecord record : csvRecords) {
		String name = record.get("Required Input");
			String pattern = "(.*)(<.*>)(.*)";
		    Matcher matcher = Pattern.compile(pattern).matcher(name);
		    
			if (matcher.matches()) {
				String templateNamePre = matcher.group(0);
				String templateNamePost = matcher.group(2);
				String groupName = matcher.group(1);
				groupName = groupName.substring(1, groupName.length() - 1); //Remove the carets
				List<Integer> groupIds = ItemData.getGroupIds(groupName);
				for (int offset = 0; offset < groupIds.size(); offset++) {
					int groupItemId = groupIds.get(offset);
					String groupItemName = ItemData.getNameFromId(groupItemId);
					String newItemName = templateNamePre + groupItemName + templateNamePost;
					Map<String, String> copyRecord = record.toMap();
					copyRecord.put("Item Name", newItemName);
					
					String newId = id + offset + "";
					copyRecord.put("Item Id", newId);
					
					processRecipeDataMap(copyRecord);
				}
			}
			else {
				processRecipeDataMap(record.toMap());
			}
		}
	}
	
	private static void processRecipeDataMap(Map<String, String> record) {
		
	}
	
}
