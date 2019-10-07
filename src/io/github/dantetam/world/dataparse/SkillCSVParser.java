package io.github.dantetam.world.dataparse;

import java.util.List;

import org.apache.commons.csv.CSVRecord;

import io.github.dantetam.world.civilization.SkillBook;

public class SkillCSVParser extends WorldCsvParser {

	public static void init() {
		List<CSVRecord> csvRecords = parseCsvFile("res/items/world-skills.csv");
		for (CSVRecord record: csvRecords) {
			String name = record.get("Skill Name");
			if (name.isBlank()) {
				continue;
			}
			
			String isCoreSkillStr = record.get("IsCoreSkill");
			boolean isCoreSkill = isCoreSkillStr != null && !isCoreSkillStr.isBlank();
			
			SkillData.addSkill(name, isCoreSkill);
			
			String abbrev = record.get("Abbreviation");
			if (!abbrev.isBlank()) {
				if (!SkillBook.skillAbbrev.containsKey(abbrev)) {
					SkillBook.skillAbbrev.put(abbrev, name);
				}
			}
		}
	}
	
}
