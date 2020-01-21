package io.github.dantetam.world.life;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.world.combat.CombatData;
import io.github.dantetam.world.combat.CombatEngine;
import io.github.dantetam.world.combat.CombatMod;
import io.github.dantetam.world.items.CombatItem;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.AnatomyData.BodyTrait;

public class Body {
	private Map<String, BodyPart> bodyParts = new HashMap<>();
	
	private Map<String, BodyTrait> bodyTraits = new HashMap<>();
	
	public String highLevelSpeciesName;
	
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
		this.highLevelSpeciesName = className;
		Set<BodyPart> speciesBodyParts = AnatomyData.getBeingNameAnatomy(className);
		for (BodyPart speciesBodyPart: speciesBodyParts) {
			bodyParts.put(speciesBodyPart.name, speciesBodyPart);
		}
	}
	
	public Set<BodyPart> getNeighborBodyParts(String bodyPart) {
		BodyPart part = this.bodyParts.get(bodyPart);
		return part.neighboringParts;
	}
	
	public Collection<BodyPart> getAllBodyParts() {
		return bodyParts.values();
	}
	
	public List<BodyPart> getAllDamagedBodyParts() {
		List<BodyPart> damaged = new ArrayList<>();
		for (BodyPart part: this.getAllBodyParts()) {
			if (part.health < part.maxHealth) {
				damaged.add(part);
			}
		}
		return damaged;
	}
	
	public int getWeaponNeed() {
		for (BodyPart bodyPart: getAllBodyParts()) {
			if (bodyPart.isMainBodyPart) {
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
			if (bodyPart.isMainBodyPart) {
				totalParts++;
				if (bodyPart.heldItems.size() > 0) {
					armoredParts++;
				}
			}
		}
		return totalParts - armoredParts;
	}
	
	public int getNumMainBodyParts() {
		int totalParts = 0;
		for (BodyPart bodyPart: getAllBodyParts()) {
			if (bodyPart.isMainBodyPart) {
				totalParts++;
			}
		}
		return totalParts;
	}
	
	public boolean canWearClothes(InventoryItem clothing) {
		Set<String> bodyParts = CombatData.getBodyPartsCover(clothing.itemId);
		return canWearClothes(bodyParts, new CombatItem(clothing));
	}
	
	public void equipCombatItem(InventoryItem invItem) {
		Set<String> bodyParts = CombatData.getBodyPartsCover(invItem.itemId);
		equipCombatItem(bodyParts, new CombatItem(invItem));
	}
	
	public void takeOffCombatItem(InventoryItem invItem) {
		Set<String> bodyParts = CombatData.getBodyPartsCover(invItem.itemId); 
		takeOffCombatItem(bodyParts, new CombatItem(invItem));
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