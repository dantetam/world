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
	public Map<String, Integer> origBuildCounts, desiredBuildCounts; 
	public Map<String, Integer> origBuildCountsOpt, desiredBuildCountsOpt;
	public Map<String, Integer> origItemStorage, desiredItemStorage;
	public Map<String, Integer> origItemStorageOpt, desiredItemStorageOpt;
	
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
	
	public void initBuildStoreNeeds(Map<String, Integer> placeBuilds, Map<String, Integer> optBuilds,
			Map<String, Integer> reqStorage, Map<String, Integer> optStorage) {
		this.origBuildCounts = placeBuilds;
		this.origBuildCountsOpt = optBuilds;
		this.origItemStorage = reqStorage;
		this.origItemStorageOpt = optStorage;
		
		this.desiredBuildCounts = new HashMap<>(this.origBuildCounts);
		this.desiredBuildCountsOpt = new HashMap<>(this.origBuildCountsOpt);
		this.desiredItemStorage = new HashMap<>(this.origItemStorage);
		this.desiredItemStorageOpt = new HashMap<>(this.origItemStorageOpt);
	}
	
	public AnnotatedRoom clone() {
		return this.clone(null, null, null);
	}
	public AnnotatedRoom clone(List<GridRectInterval> roomArea, Set<Vector3i> walls, Set<Vector3i> floors) {
		AnnotatedRoom room = new AnnotatedRoom(this.purpose, roomArea, walls, floors);
		initBuildStoreNeeds(this.origBuildCounts, this.origBuildCountsOpt, 
				this.origItemStorage, this.origItemStorageOpt);
		return room;
	}
	
}
