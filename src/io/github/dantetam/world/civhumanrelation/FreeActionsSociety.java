package io.github.dantetam.world.civhumanrelation;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.life.Human;

public class FreeActionsSociety {

	//TODO
	
	public Map<String, FreeAction> freeActions;
	
	public static class FreeAction {
		public String name;
		public Process process;
		public double meanDaysToHappen;
		
		public FreeAction(Process process, int meanDaysToHappen) {
			this.process = process;
			this.meanDaysToHappen = meanDaysToHappen;
		}
		
		public boolean fireChanceExecute() {
			return Math.random() < 1.0 / meanDaysToHappen;
		}
	}
	
	public void considerAllFreeActions(List<Human> freeHumans, Date date) {
		for (Entry<String, FreeAction> entry: freeActions.entrySet()) {
			if (!entry.getValue().fireChanceExecute()) continue;
			String name = entry.getKey();
			if (name.equals("formSociety")) {
				List<Human> bestGroup = EmergentSocietyCalc.calcMaxSubgroupNoLeader(
						freeHumans, date, "harmony", 4.0);
				TODO //Create a new society with these people who have agreed through their util. calc.
			}
		}
	}
	
}
