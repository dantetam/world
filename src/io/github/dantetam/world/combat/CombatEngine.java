package io.github.dantetam.world.combat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.MathAndDistrUti;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.combat.CombatMod.CombatModActor;
import io.github.dantetam.world.combat.CombatMod.CombatModCalc;
import io.github.dantetam.world.items.CombatItem;
import io.github.dantetam.world.life.Body;
import io.github.dantetam.world.life.BodyPart;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.life.AnatomyData.BodyDamage;

public class CombatEngine {

	public static final int BATTLE_PHASE_PREPARE = 10,
			BATTLE_PHASE_SHOCK = 30, BATTLE_PHASE_DANCE = 10;
	private static String BONUS_STRING_PREFIX = "_additive_bonus";
	
	public static void advanceBattle(War war, Battle battle) {
		if (battle.battlePhase == null) {
			battle.battlePhase = Battle.BattleMode.PREPARE;
			battle.battlePhaseTicksLeft = BATTLE_PHASE_PREPARE;
		}
	
		List<LivingEntity[]> combatPairs = battle.adjacentCombatEntities();
		for (LivingEntity[] combatPair: combatPairs) {
			LivingEntity attacker = combatPair[0], defender = combatPair[1];
			List<BodyPart> chosenAtkWeapons = new ArrayList<BodyPart>() {{
				add(bestBodyPartWithWeapon(attacker.body));
			}};
			List<BodyPart> chosenDefWeapons = new ArrayList<BodyPart>() {{
				add(bestBodyPartWithWeapon(attacker.body));
			}};
			calculateRandomHit(attacker.body, defender.body, chosenAtkWeapons, null);
		}
		
		//Cycle through battle modes once the phase timers are up
		battle.battlePhaseTicksLeft--;
		if (battle.battlePhaseTicksLeft <= 0) {
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
		
		battle.score += Math.random() < 0.5 ? Math.random() * 1 + 1 : Math.random() * 1 - 2;
		
		//Update the war's attacker warscore using the results of this battle,
		//and the general demographics of the battle, namely current wealth and prestige
		//compared to the societies' antebellum strengths and balance of power.
		double curAtkStr = 0, curDefStr = 0;
		for (Society society: war.getSameSide(war.warLeaderAttacker)) {
			curAtkStr += society.getTotalWealth() + society.getAllPeople().size() * 50;
		}
		for (Society society: war.getSameSide(war.warLeaderDefender)) {
			curDefStr += society.getTotalWealth() + society.getAllPeople().size() * 50;
		}
		double deltaAtkStr = war.startingAtkStrength - curAtkStr, 
				deltaDefStr = war.startingDefStrength - curDefStr;
		
		war.warscoreAttacker += MathAndDistrUti.clamp(deltaAtkStr - deltaDefStr, -10, 10);
	}
	
	public static void calculateDamageHealth(Body body) {
		for (BodyPart bodyPart: body.getAllBodyParts()) {
			double damageVal = bodyPart.getDamageValue() * Math.random();
			damageBodyPart(body, bodyPart, damageVal);
		}
	}
	private static void damageBodyPart(Body body, BodyPart bodyPart, double damageVal) {
		if (damageVal > 0) {
			bodyPart.health -= damageVal;
			if (bodyPart.health <= 0) {
				//Add damage to neighboring body parts, that potentially spreads out
				Set<BodyPart> neighbors = body.getNeighborBodyParts(bodyPart.name);
				for (BodyPart neighbor: neighbors) {
					damageBodyPart(body, neighbor, damageVal * Math.random() * 0.3);
				}
			}
			body.health -= damageVal;
		}
	}
	
	public static Map<String, String> getCombatStats(Body body, String actorType) {
		Map<String, String> allStats = new HashMap<>();
		
		Map<String, Double> baseStats = new HashMap<>();
	
		allStats.put("Is_Combat_Style/" + body.combatStyle, "Y");
		allStats.put("Is" + actorType, "Y");
		
		for (CombatItem combatItem: body.allItems) {
			Map<String, Double> itemStats = CombatData.combatStatsByItemIds.get(combatItem.combatItemId);
			for (Entry<String, Double> entry: itemStats.entrySet()) {
				MapUtil.addNumMap(baseStats, entry.getKey(), entry.getValue());
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
	
	public static Map<String, String> getItemStats(CombatItem item) {
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
	public static Map<Double[], Double> weaponStrikeEffByChance = new HashMap<Double[], Double>() {{
		put(new Double[] {0.0, 0.3}, 0.17);
		put(new Double[] {0.0}, 0.74);
		put(new Double[] {0.4, 1.2}, 0.06);
		put(new Double[] {1.0, 2.0}, 0.03);
	}};
	
	public static void calculateRandomHit(Body bodyAttacker, Body bodyDefender, 
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
		
		Map<CombatItem, Map<String, String>> calcItemStats = new HashMap<>();
		
		for (CombatItem combatItem: bodyAttacker.allItems) {
			List<CombatMod> itemMods = CombatData.itemCombatMods.get(combatItem.combatItemId);
			
			if (!calcItemStats.containsKey(combatItem)) {
				calcItemStats.put(combatItem, getItemStats(combatItem));
			}
			Map<String, String> itemStats = calcItemStats.get(combatItem);
			
			for (CombatMod mod: itemMods) {
				if (mod.allSatisfied(atkStats, defStats, atkStats, defStats)) {
					applyEffect(mod, mod.effectActor, atkStats, defStats, atkStats, defStats, itemStats);
				}
			}
		}
		for (CombatItem combatItem: bodyDefender.allItems) {
			List<CombatMod> itemMods = CombatData.itemCombatMods.get(combatItem.combatItemId);
			
			if (!calcItemStats.containsKey(combatItem)) {
				calcItemStats.put(combatItem, getItemStats(combatItem));
			}
			Map<String, String> itemStats = calcItemStats.get(combatItem);
			
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
			Map<String, String> weaponStats = calcItemStats.get(weapon);
			
			BodyPart attemptHit = bodyDefender.getRandomBodyPartObj(); 
			CombatItem armor = getWeaponPrecedent(attemptHit);
			Map<String, String> armorStats = calcItemStats.get(armor);
			
			double weaponMeleeAtk = getAdjVal(weaponStats, "Melee Attack");
			double weaponMeleeDefence = getAdjVal(weaponStats, "Melee Defence");
			double weaponSpeed = getAdjVal(weaponStats, "Manuever");
			
			double armorMeleeAtk = getAdjVal(armorStats, "Melee Attack");
			double armorMeleeDefence = getAdjVal(armorStats, "Melee Defence");
			double armorVal = getAdjVal(armorStats, "Armor");
			double armorSpeed = getAdjVal(armorStats, "Manuever");
			
			double weaponHit = new NormalDistribution(weaponMeleeAtk, weaponMeleeAtk * 0.1).sample();
			Double[] weaponStrikeEff = MapUtil.randChoiceFromWeightMap(weaponStrikeEffByChance);
			double weaponStrike = new UniformRealDistribution(
					weaponStrikeEff[0], weaponStrikeEff[1]
					).sample();
			
			double dmg = (weaponHit * weaponStrike) / (armorMeleeDefence + armorVal);
			if (dmg > 0) {
				attemptHit.damages.add(new BodyDamage("Weapon Strike", dmg, dmg));
			}
		}
	}
	
	private static double getAdjVal(Map<String, String> stringData, String key) {
		if (stringData.containsKey(key)) {
			double num = Double.parseDouble(stringData.get(key));
			String newKey = BONUS_STRING_PREFIX + key;
			if (stringData.containsKey(newKey)) {
				double nonMultiBonus = Double.parseDouble(stringData.get(newKey));
				num += nonMultiBonus;
			}
			return num;
		}
		else {
			throw new IllegalArgumentException("Could not find key: " + key + " in map: " + stringData);
		}
	}
	
	private static String[] desiredScoreStats = {"Melee Attack", "Melee Defense", "Ranged Attack", "Armor", "Manuever"};
	public static CombatItem getWeaponPrecedent(BodyPart part) {
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
	
	private static Map<String, Double> desiredWepStats = new HashMap<String, Double>() {{
		put("Melee Attack", 1.0);
		put("Melee Defense", 0.4);
		put("Ranged Attack", 1.0);
	}};
	public static BodyPart bestBodyPartWithWeapon(Body body) {
		Collection<BodyPart> bodyParts = body.getAllBodyParts();
		BodyPart best = null;
		double bestScore = 0;
		for (BodyPart bodyPart: bodyParts) {
			double score = 0;
			CombatItem weapon = getWeaponPrecedent(bodyPart);
			Map<String, Double> itemBaseStats = CombatData.combatStatsByItemIds.get(weapon.combatItemId);
			for (Entry<String, Double> entry: desiredWepStats.entrySet()) {
				score += entry.getValue() * itemBaseStats.get(entry.getKey());
			}
			if (score > bestScore || best == null) {
				best = bodyPart;
				bestScore = score;
			}
		}
		return best;
	}
	
	private static void applyEffect(CombatMod mod, CombatModActor mode, 
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
	
	private static void applyEffectSingle(CombatMod mod, Map<String, String> target) {
		String newEffectKey = BONUS_STRING_PREFIX + mod.effectKey;
		String origValue = target.get(mod.effectKey);
		double origDblValue = Double.parseDouble(origValue);
		if (mod.calcMode == CombatModCalc.MULTI_PERCENT) {
			target.put(mod.effectKey, (origDblValue * (1 + mod.data)) + "");
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
