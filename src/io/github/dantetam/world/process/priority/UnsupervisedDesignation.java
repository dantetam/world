package io.github.dantetam.world.process.priority;

/**
 * This class is a signal to move a process from supervised to unsupervised, 
 * i.e. the human no longer needs to participate at this stage to continue this exact stage,
 * of a process.
 * 
 * @author Dante
 *
 */

public class UnsupervisedDesignation extends Priority {
	
	public UnsupervisedDesignation() {
		super(null);
	}

}
