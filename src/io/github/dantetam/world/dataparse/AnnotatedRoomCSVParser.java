package io.github.dantetam.world.dataparse;

import java.util.List;

import org.apache.commons.csv.CSVRecord;

public class AnnotatedRoomCSVParser extends WorldCsvParser {

	public static void init() {
		List<CSVRecord> csvRecords = parseCsvFile("res/items/world-annotatedrooms.csv");
		for (CSVRecord record: csvRecords) {
			String name = record.get("Skill Name");
			TODO;
		}
	}
	
}
