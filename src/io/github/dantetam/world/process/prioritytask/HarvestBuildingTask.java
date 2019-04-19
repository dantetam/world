package io.github.dantetam.world.process.prioritytask;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.grid.LocalBuilding;

public class HarvestBuildingTask extends Task {

	public LocalBuilding buildingTarget;
	public Vector3i pickupCoords;
	
	public HarvestBuildingTask(int time, Vector3i coords, LocalBuilding target) {
		super(time);
		pickupCoords = coords;
		buildingTarget = target;
	}
	
}
