package io.github.dantetam.world.combat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.dantetam.toolbox.MathUti;
import io.github.dantetam.world.combat.CombatMod.CombatCondition;
import io.github.dantetam.world.combat.CombatMod.CombatModActor;
import io.github.dantetam.world.combat.CombatMod.CombatModCalc;
import io.github.dantetam.world.dataparse.AnatomyData.Body;
import io.github.dantetam.world.dataparse.AnatomyData.BodyPart;
import io.github.dantetam.world.dataparse.CombatItem;

public class CombatEngine {

	public static final int BATTLE_PHASE_PREPARE = 10,
			BATTLE_PHASE_SHOCK = 30, BATTLE_PHASE_DANCE = 10;
	
	public static void advanceBattle(Battle battle) {
		if (battle.battlePhase == null) {
			battle.battlePhase = Battle.BattleMode.PREPARE;
			battle.battlePhaseTicksLeft = BATTLE_PHASE_PREPARE;
		}
	
		
		
		//Cycle through battle modes once the phase timers are up
		battle.battlePhaseTicksLeft--;
		if (battle.battlePhaseTicksLeft == 0) {
			if (battle.battlePhase == Battle.BattleMode.PREPARE) {
				battle.battlePhase = Battle.BattleMode.SHOCK;
				battle.battlePhaseTicksLeft = BATTLE_PHASE_SHOCK;
			}
			else if (battle.battlePhase == Battle.BattleMode.SHOCK) {
				battle.battlePhase = Battle.BattleMode.DANCE;
				battle.battlePhaseTicksLeft = BATTLE_PHASE_DANCE;
			}
			else {
				battle.battlePhase = Battle.BattleMode.PREPARE;
				battle.battlePhaseTicksLeft = BATTLE_PHASE_PREPARE;
			}
		}
	}
	
	public Map<String, String> getCombatStats(Body body, String actorType) {
		Map<String, String> allStats = new HashMap<>();
		
		Map<String, Double> baseStats = new HashMap<>();
	
		allStats.put("IsCombatStyle/" + body.combatStyle, "Y");
		allStats.put("Is" + actorType, "Y");
		
		for (CombatItem combatItem: body.allItems) {
			Map<String, Double> itemStats = CombatData.combatStatsByItemIds.get(combatItem.combatItemId);
			for (Entry<String, Double> entry: itemStats.entrySet()) {
				MathUti.addNumMap(baseStats, entry.getKey(), entry.getValue());
			}
			allStats.put("Has_Item/" + combatItem.name, "Y");
			
			Set<String> itemStyles = CombatData.combatStylesByItemId.get(combatItem.name);
			for (String itemStyle: itemStyles) {
				allStats.put("Has_Item_Combat_Class/" + itemStyle, "Y");
			}
		}
		
		for (Entry<String, Double> numStatsEntry: baseStats.entrySet()) {
			allStats.put(numStatsEntry.getKey(), numStatsEntry.getValue() + "");
		}
		
		return allStats;
	}
	
	public void calculateRandomHit(Body bodyAttacker, Body bodyDefender, 
			List<BodyPart> chosenAtkerWeapons) {
		Map<String, String> atkStats = getCombatStats(bodyAttacker, "Atk");
		Map<String, String> defStats = getCombatStats(bodyDefender, "Def");
		
		for (CombatItem combatItem: bodyAttacker.allItems) {
			List<CombatMod> itemMods = CombatData.itemCombatMods.get(combatItem.combatItemId);
			for (CombatMod mod: itemMods) {
				if (mod.allSatisfied(atkStats, defStats, atkStats, defStats)) {
					applyEffect(mod, mod.effectActor, atkStats, defStats, atkStats, defStats, itemStats);
				}
			}
		}
		for (CombatItem combatItem: bodyDefender.allItems) {
			List<CombatMod> itemMods = CombatData.itemCombatMods.get(combatItem.combatItemId);
			for (CombatMod mod: itemMods) {
				if (mod.allSatisfied(atk, def, self, other)) {
					
				}
			}
		}
		
	}
	
	private void applyEffect(CombatMod mod, CombatModActor mode, 
			Map<String, String> atk, Map<String, String> def,
			Map<String, String> self, Map<String, String> other, Map<String, String> item) {
		switch (mode) {
		case ATTACKER:
			applyEffectSingle(mod, atk);
		case DEFENDER:
			return new HashSet<Body>() {{add(def);}};
		case SELF:
			return new HashSet<Body>() {{add(self);}};
		case OTHER:
			return new HashSet<Body>() {{add(other);}};
		case BOTH:
			return new HashSet<Body>() {{add(self); add(other);}};
		case ITEM:
			return new HashSet<CombatItem>() {{add(item);}};
		default:
			throw new IllegalArgumentException();
		}
	}
	
	private void applyEffectSingle(CombatMod mod, Map<String, String> target) {
		if (mod.calcMode == CombatModCalc.MULTI_PERCENT) {
			target.put(mod.effectKey, target.get(mod.effectKey) * mod.data);
		}
		else if (mod.calcMode == CombatModCalc.FLAT_MULTIPLICATIVE) {
			
		}
		else if (mod.calcMode == CombatModCalc.ADDITIVE_PERCENT) {
			
		}
		else if (mod.calcMode == CombatModCalc.FLAT_ADDITIVE) {
			
		}
	}
	
	public Object getAllEffectActors(CombatModActor mode, Body atk, Body def, 
			Body self, Body other) {
		
		
		switch (mode) {
		case ATTACKER:
			return new HashSet<Body>() {{add(atk);}};
		case DEFENDER:
			return new HashSet<Body>() {{add(def);}};
		case SELF:
			return new HashSet<Body>() {{add(self);}};
		case OTHER:
			return new HashSet<Body>() {{add(other);}};
		case BOTH:
			return new HashSet<Body>() {{add(self); add(other);}};
		case ITEM:
			return new HashSet<CombatItem>() {{add(item);}};
		default:
			throw new IllegalArgumentException();
		}
	}
	
	public void applyEffectToActors(CombatModActor mode, Body atk, Body def, 
			Body self, Body other, CombatItem item, Collection<CombatItems> atkItems)
	
	public boolean checkIfCombatModApplies(Body bodyAttacker, Body bodyDefender, CombatMod combatMod) {
		int numSatisfiedConds = 0;
		
		for (CombatCondition cond: combatMod.allConditions) {
			
			switch (cond.condActor) {
			case ATTACKER:
				return new HashSet<Body>() {{add(atk);}};
			case DEFENDER:
				return new HashSet<Body>() {{add(def);}};
			case SELF:
				return new HashSet<Body>() {{add(self);}};
			case OTHER:
				return new HashSet<Body>() {{add(other);}};
			case BOTH:
				return new HashSet<Body>() {{add(self); add(other);}};
			}
		}
		
		
	}
	
	public void applyEffect(Map<String, Double>)
	
	public void applyAllCombatMods(Map<String, Double> combatData, 
			List<CombatMod> allAvailableMods) {
		
	}
	
}
