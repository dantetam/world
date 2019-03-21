package io.github.dantetam.world.grid;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.worldgen.LocalGridTerrainInstantiate;

public class WorldGrid {

	public LocalGrid activeLocalGrid;
	
	public WorldGrid() {
		Vector3i sizes = new Vector3i(200,200,50);
		int biome = 3;
		activeLocalGrid = new LocalGridTerrainInstantiate(sizes, biome).setupGrid();
	}
	
}
