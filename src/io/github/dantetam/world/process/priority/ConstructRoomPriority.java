package io.github.dantetam.world.process.priority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.github.dantetam.vector.Vector3i;

public class ConstructRoomPriority extends Priority {

	public LinkedHashSet<Vector3i> allBuildingCoords; //The initial, static set of building coordinates to change
	public LinkedHashSet<Vector3i> remainingBuildCoords; //The list of tiles/building tiles in progress
	public Collection<Integer> rankedBuildMaterials;
	
	public ConstructRoomPriority(LinkedHashSet<Vector3i> buildingCoords, Collection<Integer> rankedMaterials) {
		super(null);
		remainingBuildCoords = new LinkedHashSet<>(buildingCoords);
		allBuildingCoords = new LinkedHashSet<>(buildingCoords);
		rankedBuildMaterials = rankedMaterials;
		// TODO Auto-generated constructor stub
	}

}
