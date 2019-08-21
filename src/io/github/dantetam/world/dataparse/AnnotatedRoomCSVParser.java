package io.github.dantetam.world.dataparse;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;

import org.apache.commons.csv.CSVRecord;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.Pair;
import io.github.dantetam.toolbox.StringUtil;
import io.github.dantetam.toolbox.log.CustomLog;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civilization.gridstructure.AnnotatedRoom;
import io.github.dantetam.world.civilization.gridstructure.PurposeAnnoBuildDesign;

public class AnnotatedRoomCSVParser extends WorldCsvParser {

	public static void init() {
		List<CSVRecord> csvRecords = parseCsvFile("res/items/world-annotatedrooms.csv");
		for (CSVRecord record: csvRecords) {
			if (StringUtil.validateCsvMap(record.toMap(), Arrays.asList(
					"Room Name", "Valid Complexes"
					))) {
				String roomName = record.get("Room Name"), complexName = record.get("Valid Complexes");
				
				MapUtil.insertNestedListMap(PurposeAnnoBuildDesign.complexRoomsMap, complexName, roomName);
				
				AnnotatedRoom room = new AnnotatedRoom(roomName, null, null, null);
				
				room.desiredSize = new Vector3i(3,3,1);
				String sizeStr = record.get("Target Size");
				Collection<Vector3i> vecs = parseVecs(sizeStr);
				if (!sizeStr.isBlank() || vecs.size() > 0)
					room.desiredSize = vecs.iterator().next();
				
				Map<String, Integer> placeBuilds = parseItemNumReq(record.get("Required Placed Items"));
				Map<String, Integer> optBuilds = parseItemNumReq(record.get("Optional Placed Items"));
				
				Map<String, Integer> reqStorage = parseItemNumReq(record.get("Required Storage"));
				Map<String, Integer> optStorage = parseItemNumReq(record.get("Optional Storage"));
				
				room.initBuildStoreNeeds(placeBuilds, optBuilds, reqStorage, optStorage);
				
				if (PurposeAnnoBuildDesign.allRooms.containsKey(roomName)) {
					CustomLog.errPrintln("Warning, in annotated room parsing, found multiple instances "
							+ "of room name: " + roomName);
				}
				PurposeAnnoBuildDesign.allRooms.put(roomName, room);
			}
		}
	}
	
	public static Map<String, Integer> parseItemNumReq(String totalString) {
		Map<String, Integer> itemsReqMap = new HashMap<>();
		for (String string: totalString.split(";")) {
			String[] parts = string.split(",");
			if (parts.length > 0) {
				String groupOrItemName = parts[0].strip();
				if (parts.length > 1) {
					Integer num = Integer.parseInt(parts[1].strip());
					MapUtil.addNumMap(itemsReqMap, groupOrItemName, num);
				}
				else
					MapUtil.addNumMap(itemsReqMap, groupOrItemName, -1);
			}
		}
		return itemsReqMap;
	}
	
}
