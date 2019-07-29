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
	
	public static <T, U> Map<T, U> parseMapStringIntoMap(Class<T> keyClass, Class<U> valueClass, String mapStr) {
		String[] entriesStr = mapStr.split("/");
		Map<T, U> map = new HashMap<>();
		for (String entry: entriesStr) {
			String[] pair = entry.split(",");
			if (keyClass.isInstance(new String())) {
				if (valueClass.isInstance(new String())) {
					map.put((T) pair[0], (U) pair[1]);
				}
				else {
					
				}
			}
			else {
				if (valueClass.isInstance(new String())) {
					map.put(, (U) pair[1]);
				}
				else {
					map.put((T) new Number(pair[0]), value)
				}
			}
		}
	}
	
}
