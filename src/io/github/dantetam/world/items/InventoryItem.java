package io.github.dantetam.world.items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.dantetam.world.dataparse.ItemData;
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
	
	public LivingEntity owner;
	public LivingEntity currentUser;
	
	private List<ItemSpecialProperty> itemSpecProperties;
	
	public InventoryItem(int id, int quantity, String name) {
		this.itemId = id;
		this.quantity = quantity;
		this.name = name;
		itemSpecProperties = null;
	}
	
	public InventoryItem clone() {
		return new InventoryItem(this.itemId, this.quantity, this.name);
	}
	
	public String toString() {
		return "I: [" + ItemData.getNameFromId(itemId) + ", " + quantity + "]"; 
	}
	
	public void addSpecItemProp(ItemSpecialProperty prop) {
		if (itemSpecProperties == null) {
			itemSpecProperties = new ArrayList<>();
		}
		itemSpecProperties.add(prop);
	}
	
	public ItemSpecialProperty searchSpecItemProp(String name) {
		for (ItemSpecialProperty prop: this.itemSpecProperties) {
			if (prop.getClass().getName().equals(name)) {
				return prop;
			}
		}
		return null;
	}
	
	public enum ItemQuality {
		TERRIBLE, NORMAL, GOOD, GREAT, LEGENDARY
	}
	
}
