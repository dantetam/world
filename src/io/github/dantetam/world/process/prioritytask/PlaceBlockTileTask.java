package io.github.dantetam.world.process.prioritytask;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.items.Inventory;

public class PlaceBlockTileTask extends Task {

	public Vector3i placeCoords;
	public int tileId;
	
	public PlaceBlockTileTask(int time, Vector3i coords, int tileId) {
		super(time);
		placeCoords = coords;
		this.tileId = tileId;
	}
	
}
