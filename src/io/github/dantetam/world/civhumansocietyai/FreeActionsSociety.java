package io.github.dantetam.world.civhumansocietyai;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;

import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.WorldGrid;
import io.github.dantetam.world.life.Human;

public class FreeActionsSociety {

	//TODO
	
	public static Map<String, FreeAction> freeActionsListHousehold = new HashMap<String, FreeAction>() {{
		put("formSociety", new FreeAction("formSociety", null, 50));
	}};
	
	public static Map<String, FreeAction> freeActions = new HashMap<String, FreeAction>() {{
		
	}};
	
	public static class FreeAction {
		public String name;
		public Process process;
		public double meanDaysToHappen;
		
		public FreeAction(String name, Process process, int meanDaysToHappen) {
			this.name = name;
			this.process = process;
			this.meanDaysToHappen = meanDaysToHappen;
		}
		
		public boolean fireChanceExecute() {
			return Math.random() < 1.0 / meanDaysToHappen;
		}
	}
	
	public static void considerAllFreeActions(WorldGrid world, LocalGrid grid, 
			Society society, Date date) {
		//TODO
	}
	
	public static void considerAllFreeActionsHouseholds(WorldGrid world, LocalGrid grid, 
			List<Household> freeHouseholds, Date date) {
		for (Entry<String, FreeAction> entry: freeActionsListHousehold.entrySet()) {
			if (!entry.getValue().fireChanceExecute()) continue;
			String name = entry.getKey();
			if (name.equals("formSociety")) {
				List<Household> bestGroup = EmergentSocietyCalc.calcHouseholdGen(
						freeHouseholds, date, "harmony", 4.0);
				Household hostHouse = bestGroup.get(0);
				Human host = hostHouse.headOfHousehold;
				//Create a new society with these people who have agreed through their util. calc.
				Society newSociety = new Society("NewSociety" + System.currentTimeMillis(), grid);
				newSociety.dominantCultureStr = host.dna.getDnaMapping("culture");
				for (Household house: bestGroup) {
					newSociety.addHousehold(house);
				}
				world.addSociety(newSociety);
			}
		}
	}
	
	public static void considerAllFreeActionsHumans(WorldGrid world, LocalGrid grid, 
			List<Human> humans, Date date) {
		
	}
	
	public static void considerAllFreeActions(WorldGrid world, LocalGrid grid, 
			Household house, Date date) {
		
	}
	
}
