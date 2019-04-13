package io.github.dantetam.world.process.priority;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import io.github.dantetam.vector.Vector3i;

public class ConstructRoomPriority extends Priority {

	public List<Vector3i> allBuildingCoords;
	public Collection<Integer> rankedBuildMaterials;
	
	public ConstructRoomPriority(List<Vector3i> buildingCoords, Collection<Integer> rankedMaterials) {
		super(null);
		allBuildingCoords = buildingCoords;
		rankedBuildMaterials = rankedMaterials;
		// TODO Auto-generated constructor stub
	}

}
