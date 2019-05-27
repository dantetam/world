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

public class FreeActionsSociety {

	//TODO
	
	public static Map<String, FreeAction> freeActionsInterSociety = new HashMap<String, FreeAction>() {{
		put("declareWar", new FreeAction("declareWar", null, 250));
	}};
	
	public static void considerAllFreeActions(WorldGrid world, 
			Society society, Date date) {
		TODO
	}
	
}
