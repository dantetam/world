package io.github.dantetam.world.grid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.vector.Vector3i;

/**
 * 
 * Create a record keeping system for humans to manage their buildings/compounds.
 * This architecture holds all the rooms, their positions, and their intended purposes.
 * This is used as to organize space filling algorithms by purpose.
 * 
 * e.g. a person has an annotated build designated as a home, and it has two rooms,
 * a bedroom and a kitchen/living space. The person wants to build a second bed in their bedroom,
 * so the person expands the bedroom.
 * 
 * @author Dante
 *
 */

public class PurposeAnnotatedBuild {
	
	public String totalHousePurpose;
	public Map<String, List<Room>> roomsByPurpose;
	
	public PurposeAnnotatedBuild(String totalHousePurpose) {
		this.totalHousePurpose = totalHousePurpose;
		roomsByPurpose = new HashMap<>();
	}
	
	public void addRoom(String purpose, List<GridRectInterval> roomArea, Set<Vector3i> walls, Set<Vector3i> floors) {
		Room room = new Room(purpose, roomArea, walls, floors);
		MapUtil.insertNestedListMap(this.roomsByPurpose, purpose, room);
	}
	
	public static class Room {
		public String purpose;
		public List<GridRectInterval> fullRoom; //does not include the walls of the room
		public Set<Vector3i> roomWalls;
		public Set<Vector3i> roomFloors; //Is not always a flat grid rect interval
		public Map<Vector3i, LocalBuilding> reservedBuiltSpace; //Record interior items and buildings
		
		public Room(String purpose, List<GridRectInterval> room, Set<Vector3i> walls, Set<Vector3i> floors) {
			this.purpose = purpose;
			this.fullRoom = room;
			this.roomWalls = walls;
			this.roomFloors = floors;
			this.reservedBuiltSpace = new HashMap<>();
		}
	}
	
}
