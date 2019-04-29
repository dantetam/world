package io.github.dantetam.world.dataparse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVRecord;

public class CombatCSVParser extends WorldCsvParser {
	
	private static String[] itemStatNames;
	
	public static void init() {
		itemStatNames = new String[] {"Melee Attack", "Ranged Attack", "Melee Defense", "Manuever"};
		
		List<CSVRecord> csvRecords = parseCsvFile("res/items/world-weapons.csv");
		
		for (CSVRecord record: csvRecords) {
			String name = record.get("Item Name");
			
			String stylesStr = record.get("Styles");
			String[] styles = stylesStr.split("/");
		} 
	}
	
	public static CombatMod parseCombatMod(String string) {
		
	}
	
}
