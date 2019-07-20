package io.github.dantetam.world.process.prioritytask;

import io.github.dantetam.toolbox.restricted.ReflectionUtil;

public class Task {

	public int taskTime;
	
	public Task(int time) {
		this.taskTime = time;
	}
	
	public String toString() {
		return ReflectionUtil.getDeclaredToString(this) + ", task time: " + taskTime;
	}
	
}
