package io.github.dantetam.world.combat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CombatData {

	public static Map<Integer, Map<String, Double>> combatStatsByItemIds = new HashMap<>();
	
	public static Map<String, Set<Integer>> associatedItemStyles = new HashMap<>();
	public static Map<Integer, Set<String>> combatStylesByItemId = new HashMap<>();
	
	public static Map<Integer, Set<String>> bodyPartCoverById = new HashMap<>();
	
	public static Map<Integer, List<CombatMod>> itemCombatMods = new HashMap<>();
	
	public static void initCombatItem(int id, Map<String, Double> stats, 
			String[] combatStyles, String[] bodyPartNames, List<CombatMod> combatMods) {
		combatStatsByItemIds.put(id, stats);
		
		Set<String> stylesSet = new HashSet<>();
		for (String combatStyle: combatStyles) {
			stylesSet.add(combatStyle);
			if (!associatedItemStyles.containsKey(combatStyle)) {
				associatedItemStyles.put(combatStyle, new HashSet<>());
			}
			associatedItemStyles.get(combatStyle).add(id);
		}
		combatStylesByItemId.put(id, stylesSet);
		
		Set<String> bodyPartNamesSet = new HashSet<>();
		for (String bodyPartName: bodyPartNames) {
			bodyPartNamesSet.add(bodyPartName);
		}
		bodyPartCoverById.put(id, bodyPartNamesSet);
		
		itemCombatMods.put(id, combatMods);
	}
	
	//use csv data, not hardcoded data, add content
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
	
	public static Set<String> getBodyPartsCover(int id) {
		if (!bodyPartCoverById.containsKey(id)) {
			throw new IllegalArgumentException("Could not find combat covering data for item id: " + id);
		}
		return bodyPartCoverById.get(id);
	}
	
}
