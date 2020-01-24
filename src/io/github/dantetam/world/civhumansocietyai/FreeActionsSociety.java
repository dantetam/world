package io.github.dantetam.world.civhumansocietyai;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashMap;

import io.github.dantetam.world.civhumanrelation.SocietySocietyRel;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.combat.War;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.WorldGrid;
import io.github.dantetam.world.life.Human;

public class FreeActionsSociety {
	
	//Use leadership, ethos of society, and ethos of people factoring into decisions
	
	public static Map<String, FreeAction> freeActionsInterSociety = new HashMap<String, FreeAction>() {{
		put("declareWar", new FreeAction("declareWar", null, 250));
		put("tradeSession", new FreeAction("tradeSession", null, 30));
		
		//put("surrenderInAllLosingWars", new FreeAction("surrenderInAllLosingWars", null, 1)); 
		//use this for diplomatic actions to leave wars early and separately
	}};
	
	public static void considerAllFreeActions(WorldGrid world, 
			Society host, Date date) {
		for (Entry<String, FreeAction> actionEntry: freeActionsInterSociety.entrySet()) {
			if (!actionEntry.getValue().fireChanceExecute()) continue;
			String name = actionEntry.getKey();
			
			//For all societies with valid relations, check these actions
			Map<Society, SocietySocietyRel> hostRel = world.societalDiplomacy.getInterSocietalRel(host);
			if (hostRel != null) {
				if (name.equals("declareWar")) {
					for (Entry<Society, SocietySocietyRel> entry: hostRel.entrySet()) {
						Society otherSociety = entry.getKey();
						double util = calcSocietalRelPros(world, host, otherSociety, date);
						if (util < -2.5) {
							world.societalDiplomacy.declareWar(host, otherSociety);
						}
					}
				}
				else if (name.equals("tradeSession")) {
					for (Entry<Society, SocietySocietyRel> entry: hostRel.entrySet()) {
						Society otherSociety = entry.getKey();
						double util = calcSocietalRelPros(world, host, otherSociety, date);
						if (util > 0.25) {
							world.societalDiplomacy.initiateTrade(host, otherSociety);
						}
					}
				}
				else if (name.equals("surrenderInAllLosingWars")) {
					//TODO;
				}
			}
		}
	}
	
	public static boolean callAllyToArms(WorldGrid world, Society societyCallToArms, Society ally, Society target, 
			boolean isDoubleAllied, Date date) {
		if (societyCallToArms.equals(ally) || ally.equals(target) || target.equals(societyCallToArms)) {
			throw new IllegalArgumentException("Tried to call ally relationship where one society has invalid circular/duplicate ref.");
		}
		double relCaller = calcSocietalRelPros(world, ally, societyCallToArms, date);
		double relTarget = calcSocietalRelPros(world, ally, target, date);
		double relDiff = relCaller - relTarget;
		
		double conflictBalWealthDiff = societyCallToArms.getTotalWealth() / target.getTotalWealth();
		double allyImpactWealthDiff = ally.getTotalWealth() / (societyCallToArms.getTotalWealth() - target.getTotalWealth());
	
		conflictBalWealthDiff = Math.pow(conflictBalWealthDiff - 1, 2);
		allyImpactWealthDiff = Math.pow(allyImpactWealthDiff - 0.5, 2);
		
		double util = relDiff + conflictBalWealthDiff - allyImpactWealthDiff;
		
		if (isDoubleAllied) {
			return util > 2.5;
		}
		else {
			return util > -0.5;
		}
	}
	
	public static double calcSocietalRelPros(WorldGrid world, Society host, Society otherSociety, 
			Date date) {
		SocietySocietyRel oneWayRel = world.societalDiplomacy.getInterSocietalRel(host, otherSociety);
		double util = 0;
		if (oneWayRel != null) {
			oneWayRel.reevaluateOpinion(date);
			double opinion = oneWayRel.opinion / 50;
			util = PropensityUtil.nonlinearRelUtil(opinion);
		}
		return util;
	}
	
}
