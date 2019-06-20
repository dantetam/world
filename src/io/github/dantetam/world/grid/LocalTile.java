package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.List;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.LivingEntity;

public class LocalTile {
	
	public Vector3i coords;
	
	public Human humanClaim;
	public LocalBuilding building;
	private List<LivingEntity> people;
	
	public int tileBlockId; //Representing the block which occupies this position
		//Note if filled is false, this holds the previous tile data 
		//(e.g. recording that a hole was dug into this dirt).
	public int tileFloorId; //Representing the floor below the block (the block may or may not exist)
	public boolean exposedToAir = false;
	public Inventory itemsOnFloor;
	
	public boolean harvestInUse = false;
	
	public LocalTile(Vector3i coords) {
		this.coords = coords;
		tileBlockId = ItemData.ITEM_EMPTY_ID;
		tileFloorId = ItemData.ITEM_EMPTY_ID;
		itemsOnFloor = new Inventory();
	}
	
	public void addPerson(LivingEntity person) {
		if (people == null) {
			people = new ArrayList<>();
		}
		people.add(person);
	}
	
	public void removePerson(LivingEntity person) {
		people.remove(person);
		if (people.size() == 0) {
			people = null;
		}
	}
	
	public List<LivingEntity> getPeople() {
		if (people == null) {
			return new ArrayList<>();
		}
		return people;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof LocalTile)) return false;
		return ((LocalTile) other).coords.equals(this.coords);
	}
	
	public String toString() {
		return "Tile: " + this.coords.toString() + ", block: " + ItemData.getNameFromId(this.tileBlockId);
	}
	
	/*
	public boolean isOccupied() {
		return tileBlockId != ItemData.ITEM_EMPTY_ID || tileFloorId != ItemData.ITEM_EMPTY_ID || people.size() > 0;
	}
	*/
	
}
