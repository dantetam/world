package io.github.dantetam.world.process;

import io.github.dantetam.world.life.Human;

/**
 * 
 * @author Dante
 *
 * TODO: Unique job roles in a Map<String, List<Human>> structure
 *
 */

public class LocalJob {

	public Human boss;
	public LocalProcess jobWorkProcess;
	public double estimatedUtilJob; //What the employer believes this job to be worth
	public int desiredRepetitions;
	public double shareProfitToWages; //The share of profit (util) that should be paid out to one worker
	
	public LocalJob(Human boss, LocalProcess jobWorkProcess, double estimatedUtilJob, int rep, double wage) {
		this.boss = boss;
		this.jobWorkProcess = jobWorkProcess;
		this.estimatedUtilJob = estimatedUtilJob;
		this.desiredRepetitions = rep;
		this.shareProfitToWages = wage;
	}
	
}
