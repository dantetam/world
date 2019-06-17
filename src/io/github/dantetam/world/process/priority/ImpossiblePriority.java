package io.github.dantetam.world.process.priority;

public class ImpossiblePriority extends Priority {

	public String reason;
	
	public ImpossiblePriority(String reason) {
		super(null);
		this.reason = reason;
	}
	
	@Override
	public String toString() {
		return super.toString() + ", reason: " + reason;
	}

}
