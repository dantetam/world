package io.github.dantetam.world.process;

import io.github.dantetam.world.life.Human;

public class LocalJob {

	public Human boss;
	public LocalProcess jobWorkProcess;
	public double estimatedUtilJob; //What the employer believes this job to be worth
	public int desiredRepetitions;
	public double shareProfitToWages; //The share of profit (util) that should be paid out to one worker
		//Handle multiple worker cases later? TODO
	
	public LocalJob(Human boss, LocalProcess jobWorkProcess, double estimatedUtilJob, int rep, double wage) {
		this.boss = boss;
		this.jobWorkProcess = jobWorkProcess;
		this.estimatedUtilJob = estimatedUtilJob;
		this.desiredRepetitions = rep;
		this.shareProfitToWages = wage;
	}
	
}
