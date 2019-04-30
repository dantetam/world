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
	
	public void calculateRandomHit(Body bodyAttacker, Body bodyDefender, 
			List<BodyPart> chosenAtkerWeapons) {
		Map<String, Double> atkBaseStats = new HashMap<String, Double>();
		Map<String, Double> defBaseStats = new HashMap<String, Double>();
		
		List<CombatMod> combatMods = new ArrayList<>();
		
		for (CombatItem combatItem: bodyAttacker.allItems) {
			Map<String, Double> itemStats = CombatData.combatStatsByItemIds.get(combatItem.combatItemId);
			for (Entry<String, Double> entry: itemStats.entrySet()) {
				MathUti.addNumMap(atkBaseStats, entry.getKey(), entry.getValue());
			}
		}
		for (CombatItem combatItem: bodyDefender.allItems) {
			Map<String, Double> itemStats = CombatData.combatStatsByItemIds.get(combatItem.combatItemId);
			for (Entry<String, Double> entry: itemStats.entrySet()) {
				MathUti.addNumMap(defBaseStats, entry.getKey(), entry.getValue());
			}
		}
		
		for (CombatMod mod: bodyAttacker.activePersonCombatModifiers) {
			if (mod.allSatisfied(atk, def, self, other)) {
				
			}
		}
		for (CombatMod mod: bodyDefender.activePersonCombatModifiers) {
			if (mod.allSatisfied(atk, def, self, other)) {
				
			}
		}
		
		for (CombatItem combatItem: bodyAttacker.allItems) {
			List<CombatMod> itemMods = CombatData.itemCombatMods.get(combatItem.combatItemId);
			for (CombatMod mod: itemMods) {
				if (mod.allSatisfied(atk, def, self, other)) {
					
				}
			}
		}
		for (CombatItem combatItem: bodyDefender.allItems) {
			
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
	
	public void applyAllCombatMods(Map<String, Double> combatData, List<CombatMod> allAvailableMods) {
		
	}
	
}
