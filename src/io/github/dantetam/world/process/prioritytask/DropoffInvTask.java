package io.github.dantetam.world.process.prioritytask;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.items.Inventory;

public class DropoffInvTask extends Task {

	public Inventory dropItems;
	public Vector3i dropoffCoord;
	
	public DropoffInvTask(int time, Vector3i coords, Inventory inv) {
		super(time);
		dropoffCoord = coords;
		dropItems = inv;
	}
	
}
