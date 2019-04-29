package io.github.dantetam.world.combat;

import java.util.Map;

public class CombatMod {

	//This combat modifier is applied to an item and its wearer, the 'host'.
	//In combat, when for all (condKey, condValue) entries in allCondsNecessary,
	//either OR(host[condKey] == condValue, ...) or AND(host[condKey] == condValue, ...),
	//the multiplier or flat addition
	
	public String effectKey;
	public CombatModCalc calcMode; //Otherwise, this is a flat modifier
	public double data;
	public Map<String, String> condMods;
	public boolean allCondsNecessary = true;
	
	public CombatMod(String effectKey, CombatModCalc calcMode, double data, Map<String, String> condMods) {
		this(effectKey, calcMode, data, condMods, false);
	}
	
	public CombatMod(String effectKey, CombatModCalc calcMode, double data, Map<String, String> condMods, 
			boolean allCondsNecessary) {
		this.effectKey = effectKey;
		this.calcMode = calcMode;
		this.data = data;
		this.condMods = condMods;
		this.allCondsNecessary = allCondsNecessary;
	}
	
	/**
	 * This details the various types of calculation in compiling all combat mods,
	 * ADDITIVE_PERCENT   Add percentage points, calculating only on 'base' value, like a fixed principal
	 * MULTI_PERCENT      Add percent that can multiply with other percentages, like compound interest
	 * FLAT_ADD			  Add a final, uniform amount
	 *
	 */
	public static enum CombatModCalc {
		ADDITIVE_PERCENT, MULTI_PERCENT, FLAT_ADD
	}
	
}
