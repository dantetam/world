package io.github.dantetam.world.items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;

/**
 * Represents an item that has been 'compressed' e.g. is not a building,
 * but can still take space on the floor or someone's inventory.
 * @author Dante
 *
 * TODO: Use XML parsers to create items, recipes, and so on
 *
 */

public class InventoryItem {

	public int itemId;
	public int quantity;
	public ItemQuality quality;
	public String name;
	
	public LivingEntity owner; //Normal ownership rights
	
	/*
		Someone who often but not always has ownership access,
		who wants restrictive access (reservation) of this item.
	*/
	public LivingEntity currentUser; 
	
	public Properties itemSpecProperties;
	
	public InventoryItem(int id, int quantity, String name) {
		this.itemId = id;
		this.quantity = quantity;
		this.quality = ItemQuality.NORMAL;
		this.name = name;
		itemSpecProperties = null;
	}
	
	public InventoryItem(int id, int quantity, ItemQuality quality, String name) {
		this.itemId = id;
		this.quantity = quantity;
		this.quality = quality;
		this.name = name;
		itemSpecProperties = null;
	}
	
	public InventoryItem clone() {
		return new InventoryItem(this.itemId, this.quantity, this.quality, this.name);
	}
	
	public String toString() {
		return "I: [" + ItemData.getNameFromId(itemId) + ", " + quantity + "]"; 
	}
	
	public List<ItemProperty> searchSpecItemProp(String name) {
		return this.itemSpecProperties.getPropByName(name);
	}
	
	public boolean beingHasAccessItem(LivingEntity human, Set<LivingEntity> otherOwners) {
		if (this.currentUser != null) {
			return this.currentUser.equals(human);
		}
		else {
			if (owner != null) {
				return owner.equals(human) || (otherOwners != null && otherOwners.contains(owner));
			}
			return true;
		}
	}
	
	public enum ItemQuality {
		TERRIBLE, NORMAL, GOOD, GREAT, LEGENDARY; 
		//Note these should be in order from worst to best quality
		
		public static boolean equalOrBetter(ItemQuality goal, ItemQuality inspect) {
			if (inspect == null) {
				throw new IllegalArgumentException("Invalid ItemQuality enum input: " + inspect);
			}
			List<ItemQuality> enumList = Arrays.asList(ItemQuality.values());
			return enumList.indexOf(inspect) >= enumList.indexOf(goal);
		}
		
		public static int getIndex(ItemQuality quality) {
			return Arrays.asList(ItemQuality.values()).indexOf(quality);
		}
		
		public static String toString(ItemQuality quality) {
			switch (quality) {
				case TERRIBLE:
					return "Q--";
				case NORMAL:
					return "Q-";
				case GOOD:
					return "Q";
				case GREAT:
					return "Q+";
				case LEGENDARY:
					return "Q++";
			}
			return null;
		}
	}
	
	
	
}
