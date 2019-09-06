package io.github.dantetam.world.dataparse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.csv.CSVRecord;
import org.lwjgl.util.vector.Vector3f;

import io.github.dantetam.toolbox.CollectionUtil;
import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.world.civhumanai.Ethos;
import io.github.dantetam.world.life.AnatomyData;
import io.github.dantetam.world.life.BodyPart;

public class AnatomyCSVParser extends WorldCsvParser {

	public static void init() {
		List<CSVRecord> csvRecords = parseCsvFile("res/items/world-anatomy.csv");
		
		for (CSVRecord record: csvRecords) {
			String name = record.get("Body Part Name");
			String bodyType = record.get("Body Type");
			if (name.isBlank() || bodyType.isBlank()) {
				continue;
			}
			
			Vector3f position = parseVecSplit(record, "PosX", "PosY", "PosZ");
			
			String category = record.get("Is Main Part");
			boolean isMainBodyPart = !category.isBlank() && category.equals("Y");
			
			Map<String, Double> partValues = WorldCsvParser.parseNumbers(record,
					"Size", "Vul", "Max Health", "Dexterity", "Importance");
			
			BodyPart part = new BodyPart(name, position, isMainBodyPart, partValues.get("Size"), 
					partValues.get("Vul"), partValues.get("Max Health"), partValues.get("Dexterity"),
					partValues.get("Importance"));
			
			AnatomyData.initDataBeingNameAnatomy(bodyType, part);
		}
		
		for (CSVRecord record: csvRecords) {
			String name = record.get("Body Part Name");
			String bodyType = record.get("Body Type");
			if (name.isBlank() || bodyType.isBlank()) {
				continue;
			}
			
			String parentNormal = record.get("Parent Part");
			String parentNest = record.get("Nested Parent");
			if (!parentNormal.isBlank() && !parentNest.isBlank()) {
				throw new IllegalArgumentException("Cannot have two different body parts as parents");
			}
			
			//Check if this part has a parent
			if (!parentNormal.isBlank() || !parentNest.isBlank()) {
				String parentName = parentNormal.isBlank() ? parentNest : parentNormal;
				
				Set<BodyPart> allParts = AnatomyData.getBeingNameAnatomy(bodyType);
				BodyPart parent = CollectionUtil.searchItemCond(allParts, new Function<BodyPart, Boolean>() {
					@Override
					public Boolean apply(BodyPart t) {
						return t.name.equals(parentName);
					}
				});
				BodyPart child = CollectionUtil.searchItemCond(allParts, new Function<BodyPart, Boolean>() {
					@Override
					public Boolean apply(BodyPart t) {
						return t.name.equals(name);
					}
				});
				
				if (parent == null || child == null) {
					throw new IllegalArgumentException(allParts + "\n" +
							parent + " " + parentName + "; " + child + " " + name + " " + bodyType + "\n" +
							"CSV parsing inconsistency, see above info");
				}
				
				if (!parentNormal.isBlank()) {
					parent.addAdjacentPart(child);
				}
				else if (!parentNest.isBlank()) {
					if (parent == null || child == null) throw new IllegalArgumentException("Cannot find parent"
							+ "and child of names: " + parentNest + ", " + name);
					allParts.remove(child);
					parent.chainPartInside(child);
				}
			}
		}
	}
	
	public static Vector3f parseVecSplit(CSVRecord record, String fieldX, String fieldY, String fieldZ) {
		String valX = record.get(fieldX), valY = record.get(fieldY), valZ = record.get(fieldZ);
		try {
			float x = valX.isBlank() ? 0 : Float.parseFloat(valX), 
					y = valY.isBlank() ? 0 : Float.parseFloat(valY), 
					z = valZ.isBlank() ? 0 : Float.parseFloat(valZ);
			Vector3f vec = new Vector3f(x,y,z);
			return vec;
		} catch (Exception e) {
			throw new NumberFormatException("Cannot parse in anatomy data, float: " + 
					valX + ", " + valY + ", " + valZ);
		}
	}
	
}
