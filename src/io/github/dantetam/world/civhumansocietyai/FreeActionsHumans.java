package io.github.dantetam.world.civhumansocietyai;

import java.util.ArrayList;
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

public class FreeActionsHumans {

	public static Map<String, FreeAction> freeActionsListHuman = new HashMap<String, FreeAction>() {{
		put("formNewHouseMarriage", new FreeAction("formNewHouseMarriage", null, 10));
	}};
	
	public static void considerAllFreeActionsHumans(WorldGrid world, 
			List<Human> humans, Date date) {
		for (Entry<String, FreeAction> entry: freeActionsListHuman.entrySet()) {
			if (!entry.getValue().fireChanceExecute()) continue;
			String name = entry.getKey();
			if (name.equals("formNewHouseMarriage")) {
				List<Human[]> marriagePairs = SocietalHumansActionsCalc.possibleMarriagePairs(humans, date);
				if (marriagePairs.size() == 0) continue;
				int randIndex = (int) (Math.random() * marriagePairs.size());
				Human[] pair = marriagePairs.get(randIndex);
				Human proposer = pair[0], target = pair[1];
				//Try a marriage attempt between these two people, such that these two people
				//form a new household.
				if (SocietalHumansActionsCalc.proposeMarriage(proposer, target)) {
					Household houseA = proposer.household;
					Household houseB = target.household;
					
					List<Human> allHumansHouseC = new ArrayList<>();
					if (!proposer.equals(houseA.headOfHousehold)) {
						List<List<Human>> newHousesDataA = EmergentHouseholdCalc.divideHouseholdsBySeparating(
								houseA, new ArrayList<Human>() {{add(proposer); add(target);}}, date);
						List<Human> movingOutOfA = newHousesDataA.get(1);
						houseA.removePeopleOutHouse(movingOutOfA);
						allHumansHouseC.addAll(movingOutOfA);
					}
					if (!houseB.equals(houseA) && !target.equals(houseB.headOfHousehold)) {
						List<List<Human>> newHousesDataB = EmergentHouseholdCalc.divideHouseholdsBySeparating(
								houseB, new ArrayList<Human>() {{add(proposer); add(target);}}, date);
						List<Human> movingOutOfB = newHousesDataB.get(1);
						houseB.removePeopleOutHouse(movingOutOfB);
						allHumansHouseC.addAll(movingOutOfB);
					}
					if (allHumansHouseC.size() > 0) {
						Household houseC = new Household(allHumansHouseC);
						Society host = houseA.society;
						host.addHousehold(houseC);
					}
				}
			}
		}
	}
	
}
