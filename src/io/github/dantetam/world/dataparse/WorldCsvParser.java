package io.github.dantetam.world.dataparse;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.lwjgl.util.vector.Vector3f;

import io.github.dantetam.toolbox.StringUtil;
import io.github.dantetam.vector.Vector3i;

public class WorldCsvParser {
	
	//Global initialization of all CSV parsers
	public static void init() {
		ItemCSVParser.init();
		ProcessCSVParser.init();
		CombatCSVParser.init();
		EthosCSVParser.init();
		SkillCSVParser.init();
		AnnotatedRoomCSVParser.init();
		AnatomyCSVParser.init();
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
	
	//TODO: use in parsing files for generalized
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
	
	public static List<Vector3i> parseVecs(String string) {
		List<Vector3i> results = new ArrayList<>();
		String[] vecStrs = string.split(";");
		for (String vec: vecStrs) {
			String[] numStrs = vec.split(",");
			if (numStrs.length >= 3) {
				try {
					results.add(new Vector3i(
							Integer.parseInt(numStrs[0]),
							Integer.parseInt(numStrs[1]),
							Integer.parseInt(numStrs[2])
							));
				} catch (NumberFormatException e) {
					e.printStackTrace();
					return null;
				}
			}
		}
		return results;
	}
	
	public static Map<String, Double> parseNumbers(CSVRecord record, String... fields) {
		Map<String, Double> results = new HashMap<>();
		for (String field: fields) {
			String string = record.get(field);
			Double value = string.isBlank() ? 0 : Double.parseDouble(string);
			results.put(field, value);
		}
		return results;
	}
	
	public static String getGroupNameFromStr(String name) {
		String pattern = "\\<(.*?)\\>";
	    Matcher matcher = Pattern.compile(pattern).matcher(name);
	    
		//If the group <caret> notation is present
		if (matcher.find()) {
			String groupName = matcher.group(1);
			return groupName;
		}
		else {
			return name;
		}
	}
	
}
