package io.github.dantetam.world.grid;

/**
 * Represents an item that has been 'compressed' e.g. is not a building,
 * but can still take space on the floor or someone's inventory.
 * @author Dante
 *
 * TODO: Use XML parsers to create items, recipes, and so on
 *
 */

public class Item {

	public int id;
	public int quantity;
	
	public Item(int id, int quantity) {
		this.id = id;
		this.quantity = quantity;
	}
	
}
