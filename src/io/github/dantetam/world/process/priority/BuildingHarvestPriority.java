package io.github.dantetam.world.process.priority;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.grid.LocalBuilding;

public class BuildingHarvestPriority extends Priority {
	
	public LocalBuilding building;
	
	public BuildingHarvestPriority(Vector3i coords, LocalBuilding building) {
		super(coords);
		this.building = building;
	}

}
