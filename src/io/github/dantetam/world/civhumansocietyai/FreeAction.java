package io.github.dantetam.world.civhumansocietyai;

public class FreeAction {
	public String name;
	public Process process;
	public double meanDaysToHappen;
	
	public FreeAction(String name, Process process, int meanDaysToHappen) {
		this.name = name;
		this.process = process;
		this.meanDaysToHappen = meanDaysToHappen;
	}
	
	public boolean fireChanceExecute() {
		return Math.random() < 1.0 / meanDaysToHappen;
	}
}