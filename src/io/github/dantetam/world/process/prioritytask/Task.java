package io.github.dantetam.world.process.prioritytask;

public class Task {

	public int taskTime;
	
	public Task(int time) {
		this.taskTime = time;
	}
	
	public String toString() {
		return this.getClass().getSimpleName() + "," + taskTime;
	}
	
}
