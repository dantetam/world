package io.github.dantetam.world.civhumansocietyai;

public class FreeAction {
	public String name;
	public Process process;
	public double meanMinutesToHappen;
	public int cooldownRemaining, maxCooldown;
	
	public FreeAction(String name, Process process, int meanDaysToHappen, int cooldown) {
		this.name = name;
		this.process = process;
		this.meanMinutesToHappen = meanDaysToHappen;
		this.maxCooldown = cooldown;
	}
	
	public boolean calcChanceExecute() {
		if (cooldownRemaining > 0) return false;
		return Math.random() < 1.0 / meanMinutesToHappen;
	}
	
	public void tick() {
		if (cooldownRemaining > 0) cooldownRemaining--;
	}
	
	public void conclusion() {
		cooldownRemaining = maxCooldown;
	}
	
	TODO;
	
}