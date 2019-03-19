package io.github.dantetam.world.grid;

import java.util.ArrayList;
import java.util.List;

import io.github.dantetam.vector.Vector3i;

public class LocalTile {

	public Vector3i coords;
	
	public LocalBuilding building;
	private List<Person> people;
	
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
