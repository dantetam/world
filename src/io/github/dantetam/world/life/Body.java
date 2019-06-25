package io.github.dantetam.world.life;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.world.combat.CombatEngine;
import io.github.dantetam.world.combat.CombatMod;
import io.github.dantetam.world.dataparse.AnatomyData;
import io.github.dantetam.world.dataparse.AnatomyData.BodyTrait;
import io.github.dantetam.world.dataparse.AnatomyData.MainBodyPart;
import io.github.dantetam.world.items.CombatItem;

public class Body {
	private Map<String, BodyPart> bodyParts = new HashMap<>();
	private Map<String, Set<String>> neighborBodyPartsMap = new HashMap<>();
	
	private Map<String, BodyTrait> bodyTraits = new HashMap<>();
	
	public String combatStyle;
	
	//private Map<String, List<CombatItem>> heldItemsByBodyPart = new HashMap<>();
	public Set<CombatItem> allItems;
	public Set<CombatMod> activePersonCombatModifiers;
	
	public int health, maxHealth;
	
	//The way in which this being fights, which is chosen by the AI.
	//The most efficient way for a unit to fight depends on the equipment
	//of this person, followed by the person's combat skills.
	private String fightingStyle = ""; 
	
	public Body(String className) {
		Set<BodyPart> speciesBodyParts = AnatomyData.initBeingNameAnatomy(className);
		for (BodyPart speciesBodyPart: speciesBodyParts) {
			bodyParts.put(speciesBodyPart.name, speciesBodyPart);
			neighborBodyPartsMap.put(speciesBodyPart.name, new HashSet<>());
		}
		
		Collection<String[]> neighborPairs = AnatomyData.getNeighborBodyParts(className);
		for (String[] neighborPair: neighborPairs) {
			neighborBodyPartsMap.get(neighborPair[0]).add(neighborPair[1]);
			neighborBodyPartsMap.get(neighborPair[1]).add(neighborPair[0]);
		}
	}
	
	public Set<BodyPart> getNeighborBodyParts(String bodyPart) {
		Set<BodyPart> results = new HashSet<>();
		for (String neighbor: this.neighborBodyPartsMap.get(bodyPart)) {
			results.add(bodyParts.get(neighbor));
		}
		return results;
	}
	
	public Collection<BodyPart> getAllBodyParts() {
		return bodyParts.values();
	}
	
	public int getWeaponNeed() {
		for (BodyPart bodyPart: getAllBodyParts()) {
			if (bodyPart instanceof MainBodyPart) {
				CombatItem item = CombatEngine.getWeaponPrecedent(bodyPart);
				if (item != null) {
					return 0;
				}
			}
		}
		return 1;
	}
	
	public int getArmorNeed() {
		int totalParts = 0, armoredParts = 0;
		for (BodyPart bodyPart: getAllBodyParts()) {
			if (bodyPart instanceof MainBodyPart) {
				totalParts++;
				if (bodyPart.heldItems.size() > 0) {
					armoredParts++;
				}
			}
		}
		return totalParts - armoredParts;
	}
	
	private boolean canWearClothes(Set<String> bodyPartsStrs, CombatItem clothing) {
		for (String bodyPartStr: bodyPartsStrs) {
			BodyPart bodyPartObj = this.bodyParts.get(bodyPartStr);
			if (!bodyPartObj.hasCapacity()) {
				return false;
			}
		}
		return true;
	}
	
	private void equipCombatItem(Set<String> bodyParts, CombatItem combatItem) {
		for (String bodyPartStr: bodyParts) {
			BodyPart bodyPartObj = this.bodyParts.get(bodyPartStr);
			List<CombatItem> overflowItems = bodyPartObj.wearCombatItem(combatItem);
			if (overflowItems.contains(combatItem)) { //The newest item is too big
				
			}
			else { //Some items may have been taken off to make room
				for (CombatItem overflowItem: overflowItems) {
					allItems.remove(overflowItem);
				}
			}
		}
		allItems.add(combatItem);
	}
	
	private void takeOffCombatItem(Set<String> bodyParts, CombatItem combatItem) {
		for (String bodyPartStr: bodyParts) {
			BodyPart bodyPartObj = this.bodyParts.get(bodyPartStr);
			bodyPartObj.removeCombatItem(combatItem);
		}
		allItems.remove(combatItem);
	}
	
	private String getBodyPartChance() {
		Map<String, Double> bodyPartSizes = new HashMap<>();
		for (Entry<String, BodyPart> entry: bodyParts.entrySet()) {
			BodyPart bodyPartObj = this.bodyParts.get(entry.getKey());
			bodyPartSizes.put(entry.getKey(), bodyPartObj.size * bodyPartObj.vulnerability);
		}
		String randBodyPart = MapUtil.randChoiceFromWeightMap(bodyPartSizes);
		return randBodyPart;
	}
	
	public BodyPart getRandomBodyPartObj() {
		String bodyPartStr = getBodyPartChance();
		return bodyParts.get(bodyPartStr);
	}
	
	/*
	private String getBodyPartAreaChance() {
		
	}
	*/
}