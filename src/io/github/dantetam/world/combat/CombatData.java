package io.github.dantetam.world.combat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CombatData {

	public static Map<Integer, Map<String, Double>> combatStatsByItemIds = new HashMap<>();
	public static Map<String, Set<Integer>> associatedItemStyles = new HashMap<>();
	
	public static Map<Integer, List<CombatMod>> itemCombatMods = new HashMap<>();
	
	public void initCombatItem() {
		TODO	
	}
	
	//TODO: use csv data, not hardcoded data
	public static Set<String> initBeingName(String name) {
		if (name.equals("Human")) {
			return new HashSet<String>() {{
				add("Hat");
				add("Shirt");
				add("Pants");
			}};
		}
		throw new IllegalArgumentException("Could not instantiate clothes slots for missing being type: " + name);
	}
	
}
