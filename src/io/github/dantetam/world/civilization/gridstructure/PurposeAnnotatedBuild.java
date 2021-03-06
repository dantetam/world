package io.github.dantetam.world.civilization.gridstructure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.grid.GridRectInterval;
import io.github.dantetam.world.grid.LocalBuilding;

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
	public Map<String, List<AnnotatedRoom>> roomsByPurpose;
	public Vector3i singleLocation; /** A location within the complex/anno build, used to represent it on a map **/
	
	public PurposeAnnotatedBuild(String totalHousePurpose, Vector3i location) {
		this.totalHousePurpose = totalHousePurpose;
		roomsByPurpose = new HashMap<>();
		this.singleLocation = location;
	}
	
	public void addRoom(AnnotatedRoom room) {
		MapUtil.insertNestedListMap(this.roomsByPurpose, room.purpose, room);
	}
	public void addRoom(String purpose, List<GridRectInterval> roomArea, Set<Vector3i> walls, Set<Vector3i> floors) {
		AnnotatedRoom room = new AnnotatedRoom(purpose, roomArea, walls, floors);
		MapUtil.insertNestedListMap(this.roomsByPurpose, purpose, room);
	}
	
	public boolean annoBuildContainsVec(Vector3i vec) {
		for (List<AnnotatedRoom> rooms: this.roomsByPurpose.values()) {
			for (AnnotatedRoom room: rooms) {
				if (room.containsVec(vec)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public int totalArea() {
		int curArea = 0;
		for (List<AnnotatedRoom> rooms: this.roomsByPurpose.values()) {
			for (AnnotatedRoom room: rooms) {
				curArea += room.roomFloors.size();
			}
		}
		return curArea;
	}
	
}
