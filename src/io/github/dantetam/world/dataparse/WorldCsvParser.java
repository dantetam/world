package io.github.dantetam.world.dataparse;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class WorldCsvParser {

	public static void main(String[] args) throws IOException, FileNotFoundException {
		for (CSVRecord record: parseCsvFile("res/")) {
			
		}
	}
	
	public static Iterable<CSVRecord> parseCsvFile(String fileName) {
		Reader in = null;
		try {
			in = new FileReader(fileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
			return records;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
