package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.List;

import io.github.dantetam.vector.Vector3i;

public class LocalTile {

	public Vector3i coords;
	
	public LocalBuilding building;
	private List<Person> people;
	
	public boolean filled; //Representing if this block is filled with the current tileId
	public int tileId; //Representing the block which occupies this position
		//Note if filled is false, this holds the previous tile data 
		//(e.g. recording that a hole was dug into this dirt).
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
