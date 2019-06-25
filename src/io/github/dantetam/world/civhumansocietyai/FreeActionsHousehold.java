package io.github.dantetam.world.civhumansocietyai;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;

import io.github.dantetam.world.civhumanrelation.HumanHumanRel;
import io.github.dantetam.world.civhumanrelation.HumanHumanRel.HumanHumanRelType;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.civilization.LocalExperience;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.WorldGrid;
import io.github.dantetam.world.life.Human;

public class FreeActionsHousehold {

	//TODO
	
	public static Map<String, FreeAction> freeActionsListHousehold = new HashMap<String, FreeAction>() {{
		put("formSociety", new FreeAction("formSociety", null, 50));
	}};
	
	public static Map<String, FreeAction> freeActionsListHuman = new HashMap<String, FreeAction>() {{
		put("formNewHousehold", new FreeAction("formNewHousehold", null, 50));
	}};
	
	/*
	public static void considerAllFreeActions(WorldGrid world, LocalGrid grid, 
			Society society, Date date) {
		//TODO
	}
	*/
	
	public static void considerAllFreeActionsHouseholds(WorldGrid world, LocalGrid grid, 
			List<Household> freeHouseholds, Date date) {
		for (Entry<String, FreeAction> entry: freeActionsListHousehold.entrySet()) {
			if (!entry.getValue().fireChanceExecute()) continue;
			String name = entry.getKey();
			if (name.equals("formSociety")) {
				List<Household> bestGroup = EmergentSocietyCalc.calcHouseholdGen(
						freeHouseholds, date, "harmony", 4.0);
				if (bestGroup == null) continue;
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
		//TODO
	}
	
	//For every household possible
	//society can be null if the household does not belong to a society
	public static void considerAllFreeActionsHouse(WorldGrid world, 
			Society society, Household house, Date date) {
		for (Entry<String, FreeAction> entry: freeActionsListHuman.entrySet()) {
			if (!entry.getValue().fireChanceExecute()) continue;
			String name = entry.getKey();
			if (name.equals("formNewHousehold")) {
				Human splitHuman = EmergentHouseholdCalc.calcBestHouseholdSplit(house, date);
				List<Human> separatingHumans = new ArrayList<>();
				separatingHumans.add(splitHuman);
				List<List<Human>> newHousePeople = EmergentHouseholdCalc.
						divideHouseholdsBySeparating(house, separatingHumans, date);
				//List<Human> housePeopleA = newHousePeople.get(0);
				List<Human> housePeopleB = newHousePeople.get(1);
				Household newHouse = new Household(housePeopleB);
				house.removePeopleOutHouse(housePeopleB);
				society.addHousehold(newHouse);
				
				LocalExperience newHouseExp = new LocalExperience("New Household");
				for (Human human: housePeopleB) {
					human.brain.experiences.add(newHouseExp);
				}
			}
		}
	}
	
}
