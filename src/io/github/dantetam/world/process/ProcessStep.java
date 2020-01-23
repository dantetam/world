package io.github.dantetam.world.process;

public class ProcessStep {
	public String stepType;
	public int timeTicks;
	public double modifier;
	
	public ProcessStep(String type, int time) {
		this.stepType = type;
		this.timeTicks = time;
	}
	
	public ProcessStep(String type, int time, double modifier) {
		this.stepType = type;
		this.timeTicks = time;
		this.modifier = modifier;
	}
	
	public String toString() {
		return stepType + ", " + timeTicks + ", " + modifier;
	}
}