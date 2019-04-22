package io.github.dantetam.world.process.priority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import io.github.dantetam.vector.Vector3i;

public class ConstructRoomPriority extends Priority {

	public List<Vector3i> allBuildingCoords; //The initial, static set of building coordinates to change
	public List<Vector3i> remainingBuildCoords; //The list of tiles/building tiles in progress
	public Collection<Integer> rankedBuildMaterials;
	
	public ConstructRoomPriority(List<Vector3i> buildingCoords, Collection<Integer> rankedMaterials) {
		super(null);
		remainingBuildCoords = new ArrayList<>(buildingCoords);
		allBuildingCoords = new ArrayList<>(buildingCoords);
		rankedBuildMaterials = rankedMaterials;
		// TODO Auto-generated constructor stub
	}

}
