package io.github.dantetam.world.dataparse;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.lwjgl.util.vector.Vector3f;

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
		
		private Map<String, Clothing> clothingByBodyPart = new HashMap<>();
		
		private boolean canWearClothes(Set<String> bodyParts, Clothing clothing) {
			for (String bodyPart: bodyParts) {
				if (clothingByBodyPart.containsKey(bodyPart)) {
					return false;
				}
			}
			return true;
		}
		
		private void wearClothes(Set<String> bodyParts, Clothing clothing) {
			for (String bodyPartStr: bodyParts) {
				clothingByBodyPart.put(bodyPartStr, clothing);
				//BodyPart bodyPartObj = this.bodyParts.get(bodyPartStr);
			}
		}
		
		public Collection<Clothing> getAllClothes() {
			return this.clothingByBodyPart.values();
		}
		
		private String getBodyPartChance() {
			
		}
		
		private String getBodyPartAreaChance() {
			
		}
	} 
	
	public static class BodyPart {
		public String name;
		public Vector3f position;
		public double size;
		public double vulnerability; //In terms of combat, the chance this part is hit (normalized)
		public double health, maxHealth;
		public double damage; //Blood loss, disease, and corruption that affects the whole body
	}
	
}
