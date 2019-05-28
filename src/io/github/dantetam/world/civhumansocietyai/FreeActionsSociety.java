package io.github.dantetam.world.civhumansocietyai;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;

import io.github.dantetam.world.civhumanrelation.HumanHumanRel;
import io.github.dantetam.world.civhumanrelation.SocietySocietyRel;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.WorldGrid;
import io.github.dantetam.world.life.Human;

public class FreeActionsSociety {

	//TODO
	
	/*
	public static double calcSocietalRelPros(WorldGrid world, Society host, Society otherSociety, 
			Date date) {
		
	}
	*/
	
	public static Map<String, FreeAction> freeActionsInterSociety = new HashMap<String, FreeAction>() {{
		put("declareWar", new FreeAction("declareWar", null, 250));
	}};
	
	public static void considerAllFreeActions(WorldGrid world, 
			Society host, Date date) {
		Map<Society, SocietySocietyRel> hostRel = world.societalDiplomacy.getInterSocietalRel(host);
		if (hostRel != null) {
			for (Entry<Society, SocietySocietyRel> entry: hostRel.entrySet()) {
				Society otherSociety = entry.getKey();
				SocietySocietyRel oneWayRel = world.societalDiplomacy.getInterSocietalRel(host, otherSociety);
				double util = 0;
				if (oneWayRel != null) {
					oneWayRel.reevaluateOpinion(date);
					double opinion = oneWayRel.opinion / 50;
					util = PropensityUtil.nonlinearRelUtil(opinion);
				}
				TODO
			}
		}
	}
	
}
