package io.github.dantetam.world.civilization.gridstructure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.grid.GridRectInterval;
import io.github.dantetam.world.grid.LocalBuilding;

public class AnnotatedRoom {
	
	public String purpose;
	public List<GridRectInterval> fullRoom; //does not include the walls of the room
	public Set<Vector3i> roomWalls;
	public Set<Vector3i> roomFloors; //Is not always a flat grid rect interval
	public Map<Vector3i, LocalBuilding> reservedBuiltSpace; //Record interior items and buildings
	
	//Initialized in room creation/prioritization, and modified when humans build and fill rooms
	public Vector3i desiredSize;
	public Map<Integer, Integer> origItemCounts, desiredItemCounts; 
	public Map<Integer, Integer> origItemCountsOpt, desiredItemCountsOpt;
	public Map<Integer, Integer> desiredItemStorage;
	
	public AnnotatedRoom(String purpose, List<GridRectInterval> room, Set<Vector3i> walls, Set<Vector3i> floors) {
		this.purpose = purpose;
		this.fullRoom = room;
		this.roomWalls = walls;
		this.roomFloors = floors;
		this.reservedBuiltSpace = new HashMap<>();
	}
	
	public boolean containsVec(Vector3i vec) {
		for (GridRectInterval interval: fullRoom) {
			if (interval.vecInBounds(vec)) {
				return true;
			}
		}
		return false;
	}
	
}
