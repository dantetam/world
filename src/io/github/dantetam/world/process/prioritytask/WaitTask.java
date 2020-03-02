package io.github.dantetam.world.process.prioritytask;

import io.github.dantetam.vector.Vector3i;

public class WaitTask extends Task {

	public String message;
	
	public WaitTask(int time, String message) {
		super(time);
		this.message = message;
	}
	
	public String toString() {
		return super.toString() + ", reason: " + message;
	}
	
}
