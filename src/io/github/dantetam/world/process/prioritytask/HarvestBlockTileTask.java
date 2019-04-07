package io.github.dantetam.world.process.prioritytask;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.items.Inventory;

public class HarvestBlockTileTask extends Task {

	public int tileBlockId;
	public Vector3i pickupCoords;
	
	public HarvestBlockTileTask(int time, Vector3i coords, int tileId) {
		super(time);
		tileBlockId = tileId;
		pickupCoords = coords;
	}
	
}
