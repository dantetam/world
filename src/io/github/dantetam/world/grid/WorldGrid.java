package io.github.dantetam.world.grid;

import io.github.dantetam.vector.Vector3i;

public class WorldGrid {

	public LocalGrid testGrid;
	
	public WorldGrid() {
		testGrid = new LocalGrid(new Vector3i(200,200,50));
	}
	
}
