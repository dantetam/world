package io.github.dantetam.world.process.prioritytask;

import java.util.ArrayList;

public class ImpossibleTaskPlaceholder extends ArrayList<Task> {

	public String reason;
	
	public ImpossibleTaskPlaceholder() {
		this.reason = "No reason specified";
	}
	public ImpossibleTaskPlaceholder(String reason) {
		//super();
		this.reason = reason;
	}
	
	public String toString() {
		return "ImpossibleTaskPlaceholder: " + reason;
	}
	
}
