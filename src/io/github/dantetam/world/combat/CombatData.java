package io.github.dantetam.world.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CombatData {

	public static Map<Integer, Map<String, Double>> combatStatsByItemIds = new HashMap<>();
	public static Map<String, Set<Integer>> associatedItemStyles = new HashMap<>();
	public static Map<Integer, Set<String>> associatedBodyParts = new HashMap<>(); //For storing where armor and weapons go,
		//e.g. pants go on legs and swords go into 'hands', 
		//or any part of the hand group, like hands, wooden hooks, claws, and so on.
	
	public void initCombatItem() {
		TODO	
	}
	
}
