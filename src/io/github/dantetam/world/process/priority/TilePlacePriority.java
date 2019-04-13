package io.github.dantetam.world.process.priority;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.items.Inventory;

public class TilePlacePriority extends Priority {
	
	public int materialId;
	
	public TilePlacePriority(Vector3i coords, int materialId) {
		super(coords);
		this.materialId = materialId;
	}

}
