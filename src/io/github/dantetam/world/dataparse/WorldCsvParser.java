package io.github.dantetam.world.dataparse;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class WorldCsvParser {

	public static void main(String[] args) throws IOException, FileNotFoundException {
		Reader in = new FileReader("path/to/file.csv");
		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
		for (CSVRecord record : records) {
		    System.out.println(record.toString());
		}
	}
	
}
