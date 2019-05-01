package io.github.dantetam.world.dataparse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.lwjgl.util.vector.Vector3f;

import io.github.dantetam.toolbox.MathUti;
import io.github.dantetam.world.combat.CombatMod;
import io.github.dantetam.world.items.InventoryItem;

public class AnatomyData {

	public static Map<Integer, Set<String>> associatedBodyParts = new HashMap<>(); //For storing where armor and weapons go,
		//e.g. pants go on legs and swords go into 'hands', 
		//or any part of the hand group, like hands, wooden hooks, claws, and so on.
	
	public static Set<BodyPart> initBeingNameAnatomy(String name) {
		if (name.equals("Human")) {
			return new HashSet<BodyPart>() {{
				add(new BodyPart("Left Arm", new Vector3f(-0.5f, 0f, 0f), 0.5, 1.0, 20));
				add(new BodyPart("Right Arm", new Vector3f(0.5f, 0f, 0f), 0.5, 1.0, 20));
				add(new BodyPart("Left Leg", new Vector3f(-0.2f, -0.5f, 0f), 0.5, 1.0, 20));
				add(new BodyPart("Right Leg", new Vector3f(0.2f, -0.5f, 0f), 0.5, 1.0, 20));
				add(new BodyPart("Head", new Vector3f(0f, 0.3f, 0f), 0.5, 1.0, 20));
				add(new BodyPart("Torso", new Vector3f(0f, 0f, 0f), 0.5, 1.0, 20));
			}};
		}
		throw new IllegalArgumentException("Could not instantiate anatomy slots for missing being type: " + name);
	}
	
	public static Collection<String[]> getNeighborBodyParts(String name) {
		List<String[]> neighborPairs = new ArrayList<String[]>();
		if (name.equals("Human")) {
			neighborPairs.add(new String[] {"Torso", "Left Arm"});
			neighborPairs.add(new String[] {"Torso", "Right Arm"});
			neighborPairs.add(new String[] {"Torso", "Left Leg"});
			neighborPairs.add(new String[] {"Torso", "Right Leg"});
			neighborPairs.add(new String[] {"Torso", "Head"});
		}
		else
			throw new IllegalArgumentException("Could not instantiate anatomy slots for missing being type: " + name);
		return neighborPairs;
	}
	
	public static class Body {
		private Map<String, BodyPart> bodyParts = new HashMap<>();
		private Map<String, Set<String>> neighborBodyPartsMap = new HashMap<>();
		
		public String combatStyle;
		
		//private Map<String, List<CombatItem>> heldItemsByBodyPart = new HashMap<>();
		public Set<CombatItem> allItems;
		public Set<CombatMod> activePersonCombatModifiers;
		
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
			String randBodyPart = MathUti.randChoiceFromWeightMap(bodyPartSizes);
			return randBodyPart;
		}
		
		/*
		private String getBodyPartAreaChance() {
			
		}
		*/
	} 
	
	public static class BodyPart {
		public String name;
		public Vector3f position;
		public double size;
		public double vulnerability; //In terms of combat, the chance this part is hit (normalized)
		public double health, maxHealth;
		public double damage; //Blood loss, disease, and corruption that affects the whole body
		
		public List<CombatItem> heldItems;
		public int heldItemWeightCapLeft = 1;
		public int originalWeightCap = 1;
		
		public BodyPart(String name, Vector3f position, double size, double vulnerability, double maxHealth) {
			this.name = name;
			this.position = position;
			this.size = size;
			this.vulnerability = vulnerability;
			this.maxHealth = maxHealth;
			this.health = maxHealth;
			damage = 0;
			heldItems = new ArrayList<>();
		}
		
		public boolean hasCapacity() {
			return heldItemWeightCapLeft > 0;
		}
		
		/**
		 * Wear the new item, while removing the first items until this body part is at, or under capacity
		 * @return 
		 */
		public List<CombatItem> wearCombatItem(CombatItem item) {
			List<CombatItem> itemsNotWorn = new ArrayList<>();
			if (originalWeightCap >= item.itemWeight) {
				heldItems.add(item);
				while (!hasCapacity()) {
					CombatItem removedItem = heldItems.remove(0);
					itemsNotWorn.add(removedItem);
					heldItemWeightCapLeft -= removedItem.itemWeight;
				}
			}
			else {
				itemsNotWorn.add(item);
			}
			return itemsNotWorn;
		}
		
		public void removeCombatItem(CombatItem item) {
			if (heldItems.contains(item)) {
				heldItems.remove(item);
				heldItemWeightCapLeft -= item.itemWeight;
			}
		}
	}
	
}
