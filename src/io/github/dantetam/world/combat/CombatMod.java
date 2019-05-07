package io.github.dantetam.world.combat;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.dantetam.toolbox.MathUti;
import io.github.dantetam.world.combat.CombatMod.CombatCondition;
import io.github.dantetam.world.dataparse.AnatomyData.Body;

public class CombatMod {

	//This combat modifier is applied to an item and its wearer, the 'host'.
	//In combat, when for all (condKey, condValue) entries in allCondsNecessary,
	//either OR(host[condKey] == condValue, ...) or AND(host[condKey] == condValue, ...),
	//the multiplier or flat addition
	
	public CombatModActor effectActor;
	public String effectKey;
	public CombatModCalc calcMode; //Otherwise, this is a flat modifier
	public double data;
	
	public List<CombatCondition> allConditions;
	public CombatCondLogicMode allCondsLogic = CombatCondLogicMode.ALL;
	
	public CombatMod(String effectKey, CombatModCalc calcMode, double data, 
			List<CombatCondition> allConditions) {
		this(effectKey, calcMode, data, allConditions, CombatCondLogicMode.ALL);
	}
	
	public CombatMod(String effectKey, CombatModCalc calcMode, double data, 
			List<CombatCondition> allConditions, CombatCondLogicMode allCondsLogic) {
		this.effectKey = effectKey;
		this.calcMode = calcMode;
		this.data = data;
		this.allConditions = allConditions;
		this.allCondsLogic = allCondsLogic;
	}
	
	public boolean allSatisfied(Map<String, String> atk, Map<String, String> def,
			Map<String, String> self, Map<String, String> other) {
		int numSatisfiedConds = 0;
		for (CombatCondition cond: allConditions) {
			numSatisfiedConds += cond.isSatisfied(atk, def, self, other) ? 1 : 0;
		}
		return applyCondLogic(allCondsLogic, numSatisfiedConds, allConditions.size());
	}
	
	public static class CombatCondition {
		public CombatModActor condActor;
		public Map<String, String> condMods;
		public CombatCondLogicMode condLogicMode = CombatCondLogicMode.ALL;
		
		public boolean isSatisfied(Map<String, String> atk, Map<String, String> def,
				Map<String, String> self, Map<String, String> other) {
			int numSatisfiedConds = 0;
			for (Entry<String, String> entry: condMods.entrySet()) {
				boolean check;
				
				boolean selfCheck = MathUti.checkKeyValue(self, entry.getKey(), entry.getValue());
				boolean otherCheck = MathUti.checkKeyValue(other, entry.getKey(), entry.getValue());
				
				switch (condActor) {
				case ATTACKER:
					check = MathUti.checkKeyValue(atk, entry.getKey(), entry.getValue());
					break;
				case DEFENDER:
					check = MathUti.checkKeyValue(def, entry.getKey(), entry.getValue());
					break;
				case SELF:
					check = selfCheck;
					break;
				case OTHER:
					check = otherCheck;
					break;
				case BOTH: 
					check = selfCheck && otherCheck;
					break;
				case NONE: 
					check = !selfCheck || !otherCheck;
					break;
				case EITHER: 
					check = selfCheck || otherCheck;
					break;
				case XOR:
					if (selfCheck && otherCheck) {
						check = false;
					}
					else {
						check = selfCheck || otherCheck;
					}
					break;
				default:
					throw new IllegalArgumentException("Invalid CombatModActor case for checking string data");
				}
				
				numSatisfiedConds += check ? 1 : 0;
			}
			
			return applyCondLogic(condLogicMode, numSatisfiedConds, condMods.size());
		}
	}
	
	/**
	 * This details the various types of calculation in compiling all combat mods,
	 * ADDITIVE_PERCENT   Add percentage points, calculating only on 'base' value, like a fixed principal
	 * MULTI_PERCENT      Add percent that can multiply with other percentages, like compound interest
	 * FLAT_ADDITIVE			  Add a final, uniform amount, which cannot be compounded
	 * FLAT_MULTIPLICATIVE        
	 *
	 */
	public static enum CombatModCalc {
		ADDITIVE_PERCENT, MULTI_PERCENT, FLAT_ADDITIVE, FLAT_MULTIPLICATIVE
	}
	
	/**
	 * 
	 */
	public static enum CombatModActor {
		ATTACKER, DEFENDER,
		SELF, OTHER, BOTH,
		
		//Special for requirement checks
		NONE, EITHER, XOR,
		
		//For effects only
		ITEM
	}
	
	public static enum CombatCondLogicMode {
		ALL, NONE, ANY, XOR, AT_LEAST_HALF
	}
	
	public static boolean applyCondLogic(CombatCondLogicMode condLogicMode, int numSatisfiedConds,
			int totalConds) {
		switch (condLogicMode) {
		case ALL:
			return numSatisfiedConds == totalConds;
		case NONE:
			return numSatisfiedConds == 0;
		case ANY:
			return numSatisfiedConds > 0;
		case XOR:
			return numSatisfiedConds > 0 && numSatisfiedConds <= totalConds;
		case AT_LEAST_HALF:
			return numSatisfiedConds >= totalConds / 2;
		default:
			throw new IllegalArgumentException("Invalid CombatCondLogicMode for checking all prereq string checks together");
		}
	}
	
}
