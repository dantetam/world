package io.github.dantetam.world.life;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import org.lwjgl.util.vector.Vector3f;

import io.github.dantetam.toolbox.CollectionUtil;
import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.world.dataparse.ItemCSVParser;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.ItemTotalDrops;
import io.github.dantetam.world.dataparse.ItemTotalDrops.ItemDrop;
import io.github.dantetam.world.dataparse.ItemTotalDrops.ItemDropTrial;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.ProcessStep;


public class AnatomyData {
	
	private static Map<String, Set<BodyPart>> allBodies = new HashMap<>();
	
	/*
	 * For storing where armor and weapons go, e.g. pants go on legs and swords go into 'hands', 
	 * or any part of the hand group, like hands, wooden hooks, claws, and so on.
	 */
	public static Map<Integer, Set<String>> associatedBodyParts = new HashMap<>(); 
	
	public static Set<BodyPart> getBeingNameAnatomy(String name) {
		if (allBodies.containsKey(name)) {
			return cloneBodyParts(allBodies.get(name));
		}
		throw new IllegalArgumentException("Could not instantiate anatomy slots for missing being type: " + name);
	}
	
	public static void initDataBeingNameAnatomy(String name, BodyPart bodyPart) {
		MapUtil.insertNestedSetMap(allBodies, name, bodyPart);
	}
	
	//Clone a new body with all neighbors and nested parts preserved
	private static Set<BodyPart> cloneBodyParts(Set<BodyPart> parts) {
		Set<BodyPart> results = new HashSet<>();
		for (BodyPart part: parts) {
			traverseParts(results, part);
		}
		for (BodyPart part: parts) {
			for (BodyPart neighbor: part.neighboringParts) {
				BodyPart parent = CollectionUtil.searchItemCond(results, new Function<BodyPart, Boolean>() {
					@Override
					public Boolean apply(BodyPart t) {
						return t.name.equals(part.name);
					}
				});
				BodyPart child = CollectionUtil.searchItemCond(results, new Function<BodyPart, Boolean>() {
					@Override
					public Boolean apply(BodyPart t) {
						return t.name.equals(neighbor.name);
					}
				});
				parent.addAdjacentPart(child);
			}
		}
		return results;
	}
	private static void traverseParts(Set<BodyPart> storeNewParts, BodyPart part) {
		storeNewParts.add(new BodyPart(part.name, part.position, part.isMainBodyPart, part.size, 
				part.vulnerability, part.maxHealth, part.dexterity, part.importance));
		for (BodyPart insidePart: part.insideParts) {
			traverseParts(storeNewParts, insidePart);
		}
	}
	
	/**
	 * When the Anatomy csv is parsed, that means all bodies have been completely initialized.
	 * Create the 'corpse' items for these organisms
	 */
	public static void doneParsingAllBodies() {
		for (Entry<String, Set<BodyPart>> entry: allBodies.entrySet()) {
			String animalName = entry.getKey();
			String itemName = animalName + " Body";
			Set<BodyPart> bodyParts = entry.getValue();
			double size = 0;
			for (BodyPart bodyPart: bodyParts) {
				size += bodyPart.size;
			}
			final double totalSize = size;
			ItemTotalDrops dropOnHarvest = ItemCSVParser.processItemDropsString(itemName);
			
			ItemData.addItemToDatabase(
					ItemData.generateIdNoNewItem(), 
					itemName, 
					true, 
					new String[] {"Body"}, 
					null, 
					ItemData.ITEM_EMPTY_ID, 
					dropOnHarvest, 
					5, 0, 0.5, 
					new ArrayList<ProcessStep>() {{add(new ProcessStep("Fuel", 0, totalSize));}}, 
					new ArrayList<ProcessStep>() {{}}, 
					null);
		}
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
