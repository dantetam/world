package io.github.dantetam.world.process.prioritytask;

import io.github.dantetam.vector.Vector3i;

public class MoveTask extends Task {

	public Vector3i coordsFrom;
	public Vector3i coordsDest;
	
	public MoveTask(int time, Vector3i from, Vector3i dest) {
		super(time);
		coordsFrom = from;
		coordsDest = dest;
	}
	
}
