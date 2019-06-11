package io.github.dantetam.world.dataparse;

import java.util.List;

import org.apache.commons.csv.CSVRecord;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.world.civhumanai.Ethos;

public class EthosCSVParser extends WorldCsvParser {

	public static void init() {
		List<CSVRecord> csvRecords = parseCsvFile("res/items/world-ethos.csv");
		for (CSVRecord record: csvRecords) {
			String name = record.get("Ethos Name");
			if (name.isBlank()) {
				continue;
			}
			
			String effectsString = record.get("Effects String");
			Ethos ethos = new Ethos(name, 0, "", effectsString);
			
			String category = record.get("IsBigTrait");
			if (!category.isBlank() && category.equals("Y")) {
				EthosData.initGreatEthos().put(name, ethos);
			}
			else {
				EthosData.initPersonalTraits().put(name, ethos);
			}
			
			String oppositeNamesString = record.get("Ethos Opposite");
			if (!oppositeNamesString.isBlank()) {
				String[] oppositeNames = oppositeNamesString.split("/");
				for (String otherEthosName: oppositeNames) {
					MapUtil.insertNestedSetMap(EthosData.oppositeEthosMap, name, otherEthosName);
				}
			}
		}
	}
	
}
