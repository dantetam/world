package io.github.dantetam.world.dataparse;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.lwjgl.util.vector.Vector3f;

import io.github.dantetam.toolbox.MathUti;
import io.github.dantetam.world.items.InventoryItem;

public class AnatomyData {

	public static Set<String> initBeingName(String name) {
		if (name.equals("Human")) {
			return new HashSet<String>() {{
				add("Hat");
				add("Shirt");
				add("Pants");
			}};
		}
		throw new IllegalArgumentException("Could not instantiate clothes slots for missing being type: " + name);
	}
	
	public static class Body {
		private Map<String, BodyPart> bodyParts = new HashMap<>();
		private Map<String, Set<String>> neighborBodyPartsMap = new HashMap<>();
		
		//private Map<String, List<CombatItem>> heldItemsByBodyPart = new HashMap<>();
		
		//The way in which this being fights, which is chosen by the AI.
		//The most efficient way for a unit to fight depends on the equipment
		//of this person, followed by the person's combat skills.
		private String fightingStyle = ""; 
		
		private boolean canWearClothes(Set<String> bodyParts, CombatItem clothing) {
			for (String bodyPart: bodyParts) {
				if (heldItemsByBodyPart.get(bodyPart).contains(clothing)) {
					return false;
				}
			}
			return true;
		}
		
		private void wearClothes(Set<String> bodyParts, CombatItem clothing) {
			for (String bodyPartStr: bodyParts) {
				heldItemsByBodyPart.put(bodyPartStr, clothing);
				//BodyPart bodyPartObj = this.bodyParts.get(bodyPartStr);
			}
		}
		
		public Collection<CombatItem> getAllCombatItems() {
			return this.heldItemsByBodyPart.values();
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
		public int heldItemCapacity = 1;
		
		public BodyPart() {
			heldItems = new ArrayList<>();
		}
	}
	
}
