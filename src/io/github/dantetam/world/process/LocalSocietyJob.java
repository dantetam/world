package io.github.dantetam.world.process;

import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.life.Human;

/**
 * 
 * @author Dante
 *
 * TODO: Unique job roles in a Map<String, List<Human>> structure
 *
 */

public class LocalSocietyJob extends LocalJob {

	public Society societyJob;
	
	public LocalSocietyJob(Society societyJob, LocalProcess jobWorkProcess, 
			double estimatedUtilJob, int rep, double wage) {
		super(null, jobWorkProcess, estimatedUtilJob, rep, wage);
		this.societyJob = societyJob;
	}
	
}
