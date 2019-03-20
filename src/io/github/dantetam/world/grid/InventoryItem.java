package io.github.dantetam.world.grid;

/**
 * Represents an item that has been 'compressed' e.g. is not a building,
 * but can still take space on the floor or someone's inventory.
 * @author Dante
 *
 * TODO: Use XML parsers to create items, recipes, and so on
 *
 */

public class InventoryItem {

	public int id;
	public int quantity;
	public String name;
	
	public InventoryItem(int id, int quantity, String name) {
		this.id = id;
		this.quantity = quantity;
		this.name = name;
	}
	
}
