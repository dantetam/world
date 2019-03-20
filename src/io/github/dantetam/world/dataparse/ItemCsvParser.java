package io.github.dantetam.world.dataparse;

import org.apache.commons.csv.CSVRecord;

public class ItemCsvParser extends WorldCsvParser {

	public static void init() {
		for (CSVRecord record: parseCsvFile("res/items/world-itemideas.csv")) {
			int id = Integer.parseInt(record.get("Item Id"));
			String name = record.get("Item Name");
			boolean placeable = record.get("CanBeBlock").equals("Y");
			String[] groups = record.get("Groups").split(";");
			String stackable = record.get("Stackable");
			Integer stackNum = null;
			if (stackable.isBlank()) {
				try {
					stackNum = Integer.parseInt(stackable);
				} catch (NumberFormatException e) {
					e.printStackTrace();
					System.err.println("Could not parse stack num in item CSV parsing");
				}
			}
			ItemData.addItemToDatabase(id, name, placeable, groups, stackNum);
		}
	}
	
}
