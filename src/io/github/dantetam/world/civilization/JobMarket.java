package io.github.dantetam.world.civilization;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.github.dantetam.localdata.ConstantData;
import io.github.dantetam.world.civhumanai.Ethos;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.execute.LocalGridTimeExecution;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.process.LocalJob;
import io.github.dantetam.world.process.LocalProcess;

public class JobMarket {

	public Map<LocalProcess, LocalJob> allJobsAvailable;
	
	public JobMarket() {
		this.allJobsAvailable = new HashMap<>();
	}
	
	public void createJobOffers(Society society, LocalGrid grid) {
		allJobsAvailable.clear();
		for (Human human: society.getAllPeople()) {
			
			double wagePercentageOfOutput = 0.25;
			Ethos workPayEthos = human.brain.ethosSet.ethosEconomics.get("Worker Percentage Wages");
			if (workPayEthos != null) {
				wagePercentageOfOutput += workPayEthos.getLogisticVal(-0.2, 0.5);
			}
			
			Map<Integer, Double> calcUtility = society.findCompleteUtilityAllItems(human);
			Map<LocalProcess, Double> bestProcesses = society.prioritizeProcesses(calcUtility, grid, human, 
					ConstantData.NUM_JOBPROCESS_CONSIDER, null);
			double wealth = human.getTotalWealth();
			for (Entry<LocalProcess, Double> entry: bestProcesses.entrySet()) {
				double util = wealth / (entry.getValue() * (1 - wagePercentageOfOutput));
				util = Math.pow(util, 1.3);
				if (util > 5) {
					LocalProcess process = entry.getKey();
					int repetitions = (int) Math.ceil(1440.0 / process.totalTime());
					LocalJob newJob = new LocalJob(human, process, util, repetitions, wagePercentageOfOutput);
					allJobsAvailable.put(process, newJob);
				}
			}
		}
	}
	
	public String toString() {
		return this.allJobsAvailable.toString();
	}
	
}
