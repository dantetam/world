package io.github.dantetam.world.dataparse;

import java.util.List;

import org.apache.commons.csv.CSVRecord;

import io.github.dantetam.world.life.Ethos;

public class EthosCSVParser extends WorldCsvParser {

	public static void init() {
		//TODO
		List<CSVRecord> csvRecords = parseCsvFile("res/items/world-ethos.csv");
		for (CSVRecord record: csvRecords) {
			String name = record.get("Ethos Name");
			if (name == null || name.isBlank()) {
				continue;
			}
			
			String effectsString = record.get("Effects String");
			Ethos ethos = new Ethos(name, 0, "", effectsString);
			
			TODO;
			
			String oppositeName = record.get("Ethos Opposite");
			if (oppositeName != null && !oppositeName.isBlank()) {
				
			}
			
		}
	}
	
}
