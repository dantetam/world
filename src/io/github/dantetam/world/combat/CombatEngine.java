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
	
	public Map<String, String> getItemStats(CombatItem item) {
		Map<String, Double> itemBaseStats = CombatData.combatStatsByItemIds.get(item.combatItemId);
		Map<String, String> itemStringStats = new HashMap<>();
		for (Entry<String, Double> entry: itemBaseStats.entrySet()) {
			itemStringStats.put(entry.getKey(), entry.getValue() + "");
		}
		return itemStringStats;
	}
	
	public static double[][] weaponNumEfficiency = {
			{1.0},
			{0.5, 0.3},
			{0.2, 0.2, 0.1},
			{0.2, 0.1, 0.1, 0.1}
	};
	
	public void calculateRandomHit(Body bodyAttacker, Body bodyDefender, 
			List<BodyPart> chosenAtkerWeapons, List<String> combatType) {
		Map<String, String> atkStats = getCombatStats(bodyAttacker, "Atk");
		Map<String, String> defStats = getCombatStats(bodyDefender, "Def");
		
		for (CombatMod mod: bodyAttacker.activePersonCombatModifiers) {
			if (mod.allSatisfied(atkStats, defStats, atkStats, defStats)) {
				applyEffect(mod, mod.effectActor, atkStats, defStats, atkStats, defStats, null);
			}
		}
		for (CombatMod mod: bodyDefender.activePersonCombatModifiers) {
			if (mod.allSatisfied(atkStats, defStats, defStats, atkStats)) {
				applyEffect(mod, mod.effectActor, atkStats, defStats, defStats, atkStats, null);
			}
		}
		
		for (CombatItem combatItem: bodyAttacker.allItems) {
			List<CombatMod> itemMods = CombatData.itemCombatMods.get(combatItem.combatItemId);
			Map<String, String> itemStats = getItemStats(combatItem);
			for (CombatMod mod: itemMods) {
				if (mod.allSatisfied(atkStats, defStats, atkStats, defStats)) {
					applyEffect(mod, mod.effectActor, atkStats, defStats, atkStats, defStats, itemStats);
				}
			}
		}
		for (CombatItem combatItem: bodyDefender.allItems) {
			List<CombatMod> itemMods = CombatData.itemCombatMods.get(combatItem.combatItemId);
			Map<String, String> itemStats = getItemStats(combatItem);
			for (CombatMod mod: itemMods) {
				if (mod.allSatisfied(atkStats, defStats, defStats, atkStats)) {
					applyEffect(mod, mod.effectActor, atkStats, defStats, defStats, atkStats, itemStats);
				}
			}
		}
		
		double[] weaponEfficiency = weaponNumEfficiency[chosenAtkerWeapons.size() - 1];
		for (int weaponIndex = 0; weaponIndex < chosenAtkerWeapons.size(); weaponIndex++) {
			BodyPart bodyPart = chosenAtkerWeapons.get(weaponIndex);
			CombatItem weapon = getWeaponPrecedent(bodyPart);
			double thisWeaponEff = weaponEfficiency[weaponIndex]; 
			
			BodyPart attemptHit = bodyDefender.getRandomBodyPartObj(); 
		}
	}
	
	private static String[] desiredScoreStats = {"Melee Attack", "Melee Defense", "Ranged Attack", "Manuever"};
	public CombatItem getWeaponPrecedent(BodyPart part) {
		CombatItem candidate = null;
		double bestScore = 0;
		for (CombatItem heldItem: part.heldItems) {
			double score = 0;
			Map<String, Double> itemBaseStats = 
					CombatData.combatStatsByItemIds.get(heldItem.combatItemId);
			for (String desiredScoreStat: desiredScoreStats) {
				if (itemBaseStats.containsKey(desiredScoreStat)) {
					score += itemBaseStats.get(desiredScoreStat);
				}
			}
			if (score > bestScore || candidate == null) {
				bestScore = score;
				candidate = heldItem;
			}
		}
		return candidate;
	}
	
	private void applyEffect(CombatMod mod, CombatModActor mode, 
			Map<String, String> atk, Map<String, String> def,
			Map<String, String> self, Map<String, String> other, Map<String, String> item) {
		switch (mode) {
		case ATTACKER:
			applyEffectSingle(mod, atk);
		case DEFENDER:
			applyEffectSingle(mod, def);
		case SELF:
			applyEffectSingle(mod, self);
		case OTHER:
			applyEffectSingle(mod, other);
		case BOTH:
			applyEffectSingle(mod, self);
			applyEffectSingle(mod, other);
		case ITEM:
			applyEffectSingle(mod, item);
		default:
			throw new IllegalArgumentException();
		}
	}
	
	private void applyEffectSingle(CombatMod mod, Map<String, String> target) {
		String newEffectKey = "_additive_bonus" + mod.effectKey;
		String origValue = target.get(mod.effectKey);
		double origDblValue = Double.parseDouble(origValue);
		if (mod.calcMode == CombatModCalc.MULTI_PERCENT) {
			target.put(mod.effectKey, (origDblValue * mod.data) + "");
		}
		else if (mod.calcMode == CombatModCalc.FLAT_MULTIPLICATIVE) {
			target.put(mod.effectKey, (origDblValue + mod.data) + "");
		}
		else if (mod.calcMode == CombatModCalc.ADDITIVE_PERCENT) {
			target.put(newEffectKey, (origDblValue * mod.data) + "");
		}
		else if (mod.calcMode == CombatModCalc.FLAT_ADDITIVE) {
			target.put(newEffectKey, (origDblValue + mod.data) + "");
		}
		else {
			throw new IllegalArgumentException("Could not apply a combat mod effect with calc mode: " + mod.calcMode);
		}
	}
	
	/*
	public Object getAllEffectActors(CombatModActor mode, Body atk, Body def, 
			Body self, Body other, ) {
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
	
	public void applyEffect(Map<String, Double>)
	
	public void applyAllCombatMods(Map<String, Double> combatData, 
			List<CombatMod> allAvailableMods) {
		
	}
	*/
	
}
