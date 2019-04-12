package io.github.dantetam.world.process.priority;

import java.util.Set;

import io.github.dantetam.vector.Vector3i;

public class ConstructRoomPriority extends Priority {

	public Set<Vector3i> allBuildingCoords;
	public Set<Integer> buildingMaterials;
	
	public ConstructRoomPriority(Set<Vector3i> buildingCoords, Set<Integer> materials) {
		super(null);
		allBuildingCoords = buildingCoords;
		buildingMaterials = materials;
		// TODO Auto-generated constructor stub
	}

}
