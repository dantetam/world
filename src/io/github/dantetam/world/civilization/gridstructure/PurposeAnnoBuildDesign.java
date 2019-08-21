package io.github.dantetam.world.civilization.gridstructure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Create rooms based from either templates or those that emerge from a society and its ethos.
 * These can also be defined as rooms in CSV files.
 * 
 * Rooms are defined by a purpose at the highest level, and then have variations, in these data fields:
 * size in two dimensions, required/optional items to place, desired items to keep in storage in the room.
 * 
 * These definitions can include nested processes, e.g. a textile processing room uses the cloth spinning
 * process, which requires a loom. For more complicated 'workchains' in the future, use the process
 * data to create specialized rooms.
 * 
 * TODO: The more complicated emergence of specialized rooms
 * @author Dante
 *
 */

public class PurposeAnnoBuildDesign {
	
	//Predesigned rooms with target size, builds, items storage data
	public static Map<String, AnnotatedRoom> allRooms = new HashMap<>(); 
	
	public static Map<String, List<String>> complexRoomsMap = new HashMap<>();

	public static AnnotatedRoom getRoom(String name) {
		if (!allRooms.containsKey(name)) {
			throw new IllegalArgumentException("Purpose annotated room does not contain this name: " + name);
		}
		return allRooms.get(name);
	}
	
}
