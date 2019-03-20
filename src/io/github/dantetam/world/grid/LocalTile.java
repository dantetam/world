package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.List;

import io.github.dantetam.vector.Vector3i;

public class LocalTile {

	private static final int EMPTY_ID = -1;
	
	public Vector3i coords;
	
	public LocalBuilding building;
	private List<Person> people;
	
	public int tileBlockId; //Representing the block which occupies this position
		//Note if filled is false, this holds the previous tile data 
		//(e.g. recording that a hole was dug into this dirt).
	public int tileFloorId; //Representing the floor below the block (the block may or may not exist)
	public InventoryItem itemOnFloorId;
	
	public LocalTile(Vector3i coords) {
		this.coords = coords;
	}
	
	public void addPerson(Person person) {
		if (people == null) {
			people = new ArrayList<>();
		}
		people.add(person);
	}
	
	public void removePerson(Person person) {
		people.remove(person);
		if (people.size() == 0) {
			people = null;
		}
	}
	
	public List<Person> getPeople() {
		return people;
	}
	
}
