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
		
		for (CSVRecord record: csvRecords) {
			String name = record.get("Item Name");
			int id = ItemData.getIdFromName(name);
			
			Map<String, Double> stats = new HashMap<>();
			for (String itemStatName: itemStatNames) {
				double statValue = Double.parseDouble(record.get(itemStatName).strip());
				stats.put(itemStatName, statValue);
			}
			
			String stylesStr = record.get("Styles");
			String[] styles = stylesStr.split("/");
			
			String bodyPartsStr = record.get("Body Parts");
			String[] bodyPartNames = bodyPartsStr.split("/");
			
			String modifierString = record.get("Modifiers");
			List<CombatMod> combatMods = parseCombatMod(modifierString);
			
			CombatData.initCombatItem(id, stats, styles, bodyPartNames, combatMods);
		} 
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
