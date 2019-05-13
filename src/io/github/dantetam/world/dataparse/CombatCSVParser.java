package io.github.dantetam.world.dataparse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVRecord;

import io.github.dantetam.world.combat.CombatData;
import io.github.dantetam.world.combat.CombatMod;

public class CombatCSVParser extends WorldCsvParser {
	
	private static String[] itemStatNames;
	
	public static void init() {
		itemStatNames = new String[] {"Melee Attack", "Ranged Attack", "Melee Defense", "Manuever"};
		
		List<CSVRecord> csvRecords = parseCsvFile("res/items/world-weapons.csv");
		
		for (CSVRecord csvRecord: csvRecords) {
			Map<String, String> record = csvRecord.toMap();
			preprocessCombatItemMap(record);
		} 
	}
	
	private static void preprocessCombatItemMap(Map<String, String> record) {
		String name = record.get("Weapon Name");
		
		String pattern = "\\<(.*?)\\>";
		Matcher inputGroupMatcher = Pattern.compile(pattern).matcher(name);
		
		if (inputGroupMatcher.find()) {
			String groupName = inputGroupMatcher.group(1);
			//groupName = groupName.substring(1, groupName.length() - 1); //Remove the carets
			
			List<String> groupNames = ItemCSVParser.groupSyntaxShortcuts.get(groupName);
			if (groupNames == null) {
				System.err.println("Could not find group name: " + groupName);
			}
			else {
				for (String groupItemName: groupNames) {
					String newItemName = name.replaceFirst("\\<(.*?)\\>", groupItemName);
					
					Map<String, String> copyRecord = new HashMap<String, String>(record);
					copyRecord.put("Weapon Name", newItemName);

					preprocessCombatItemMap(copyRecord);
				}
			}
		}
		else {
			processCombatItemMap(record);
		}
	}
	
	private static void processCombatItemMap(Map<String, String> record) {
		String name = record.get("Weapon Name");
		int id = ItemData.getIdFromName(name);
		
		Map<String, Double> stats = new HashMap<>();
		for (String itemStatName: itemStatNames) {
			double statValue;
			try {
				statValue = Double.parseDouble(record.get(itemStatName).strip());
			} catch (Exception e) { //Empty value or missing column value
				statValue = 0;
			}
			stats.put(itemStatName, statValue);
		}
		
		String stylesStr = record.get("Styles");
		String[] styles = stylesStr.split("/");
		
		String bodyPartsStr = record.get("Body Parts");
		String[] bodyPartNames = bodyPartsStr.split("/");
		
		List<CombatMod> combatMods;
		try {
			String modifierString = record.get("Modifiers");
			combatMods = parseCombatMod(modifierString);
		} catch (IllegalArgumentException e) { //Missing column
			combatMods = new ArrayList<>();
		}
		
		CombatData.initCombatItem(id, stats, styles, bodyPartNames, combatMods);
	}
	
	public static List<CombatMod> parseCombatMod(String string) {
		List<CombatMod> mods = new ArrayList<>();
		String[] modStrings = string.split("/");
		for (String modString: modStrings) {
			modString = modString.strip();
			
		}
		return mods;
	}
	
}
