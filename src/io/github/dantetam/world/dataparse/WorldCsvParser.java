package io.github.dantetam.world.dataparse;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import io.github.dantetam.toolbox.StringUtil;

public class WorldCsvParser {
	
	//Global initialization of all CSV parsers
	public static void init() {
		ItemCSVParser.init();
		ProcessCSVParser.init();
		CombatCSVParser.init();
		EthosCSVParser.init();
		SkillCSVParser.init();
	}
	
	public static List<CSVRecord> parseCsvFile(String fileName) {
		Reader in = null;
		try {
			in = new FileReader(fileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord().parse(in);
			List<CSVRecord> recordsList = new ArrayList<>();
			for (CSVRecord record: records) {
				if (!record.get(0).isBlank()) {
					recordsList.add(record);
				}
			}
			return recordsList;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	//TODO: use in parsing files for 
	public static <T, U> Map<T, U> parseMapStringIntoMap(Class<T> keyClass, Class<U> valueClass, String mapStr) {
		String[] entriesStr = mapStr.split("/");
		Map<T, U> map = new HashMap<>();
		for (String entry: entriesStr) {
			String[] pair = entry.split(",");
			if (pair.length != 2) continue;
			
			boolean keyIsNumber = false;
			try {
				Double.parseDouble(pair[0]);
				keyIsNumber = true;
			} catch (NumberFormatException e) {}
			
			boolean valueIsNumber = false;
			try {
				Double.parseDouble(pair[1]);
				valueIsNumber = true;
			} catch (NumberFormatException e) {}
			
			if (keyIsNumber) {
				if (valueIsNumber) {
					map.put((T) pair[0], (U) pair[1]);
				}
				else {
					map.put((T) pair[0], (U) new Double(Double.parseDouble(pair[1])));
				}
			}
			else {
				if (valueIsNumber) {
					map.put((T) new Double(Double.parseDouble(pair[0])), (U) pair[1]);
				}
				else {
					map.put((T) new Double(Double.parseDouble(pair[0])), (U) new Double(Double.parseDouble(pair[1])));
				}
			}
		}
		return map;
	}
	
}
