package io.github.dantetam.world.grid;

import java.util.HashMap;
import java.util.Map;

public class Person {

	private int id;
	public LocalTile location;
	public String name;

	// Maps item id to item objects, for finding out quickly if this person has item x
	public Map<Integer, Item> inventory;

	public Person(String name) {
		this.name = name;
		inventory = new HashMap<>();
	}

	public boolean equals(Object other) {
		if (!(other instanceof Person)) {
			return false;
		}
		Person person = (Person) other;
		return this.id == person.id;
	}

}
