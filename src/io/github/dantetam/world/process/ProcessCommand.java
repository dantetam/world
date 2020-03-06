package io.github.dantetam.world.process;

public class ProcessCommand {

	public String commandName;
	public String[] otherData;
	
	public ProcessCommand(String commandName, String... otherData) {
		this.commandName = commandName;
		this.otherData = otherData;
	}
	
	public String toString() {
		return "ProcCommand, key: " + commandName + ", values: " + String.join(", ", otherData);
	}
	
}
