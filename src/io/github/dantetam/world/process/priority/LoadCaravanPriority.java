package io.github.dantetam.world.process.priority;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.items.Inventory;

public class LoadCaravanPriority extends Priority {

	public LocalGrid caravanStartGrid, caravanEndGrid;
	public Vector3i startVec, endVec;
	public Inventory inventory;
	
	public LoadCaravanPriority(Vector3i coords, Inventory inv) {
		super(coords);
		inventory = inv;
	}

}
