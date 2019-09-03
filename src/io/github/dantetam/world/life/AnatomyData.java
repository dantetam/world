package io.github.dantetam.world.life;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.util.vector.Vector3f;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.world.items.InventoryItem;


public class AnatomyData {
	
	private static Map<String, Set<BodyPart>> allBodies = new HashMap<>();
	
	public static Set<String> species = new HashSet<String>() {{
		add("Human");
		add("Cow");
	}};
	
	/*
	 * For storing where armor and weapons go, e.g. pants go on legs and swords go into 'hands', 
	 * or any part of the hand group, like hands, wooden hooks, claws, and so on.
	 */
	public static Map<Integer, Set<String>> associatedBodyParts = new HashMap<>(); 
	
	public static Set<BodyPart> getBeingNameAnatomy(String name) {
		if (allBodies.containsKey(name)) {
			return allBodies.get(name);
		}
		throw new IllegalArgumentException("Could not instantiate anatomy slots for missing being type: " + name);
	}
	
	public static void initDataBeingNameAnatomy(String name, BodyPart bodyPart) {
		MapUtil.insertNestedSetMap(allBodies, name, bodyPart);
	}
	
	public static class BodyDamage {
		public String name;
		public double damage;
		public double careNeeded;
		
		public BodyDamage(String name, double damage, double careNeeded) {
			this.name = name;
			this.damage = damage;
			this.careNeeded = careNeeded;
		}
	}
	
	public static class BodyTrait {
		public String traitName;
		public double traitModifier;
		
		public BodyTrait(String traitName, double traitModifier) {
			this.traitName = traitName;
			this.traitModifier = traitModifier;
		}
	}
	
}
