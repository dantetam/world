package io.github.dantetam.world.process.prioritytask;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.items.Inventory;

public class PickupTask extends Task {

	public Inventory desiredItems;
	public Vector3i pickupCoords;
	
	public PickupTask(int time, Vector3i coords, Inventory inv) {
		super(time);
		desiredItems = inv;
		pickupCoords = coords;
	}
	
}
