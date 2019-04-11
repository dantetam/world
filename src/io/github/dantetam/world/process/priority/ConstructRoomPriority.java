package io.github.dantetam.world.process.priority;

import java.util.Set;

import io.github.dantetam.vector.Vector3i;

public class ConstructRoomPriority extends Priority {

	public Set<Vector3i> allBuilding;
	public Set<Integer> buildingMaterials;
	
	public ConstructRoomPriority(Set<Vector3i> coords, Set<Integer> materials) {
		super(null);
		allBuilding = coords;
		buildingMaterials = materials;
		// TODO Auto-generated constructor stub
	}

}
