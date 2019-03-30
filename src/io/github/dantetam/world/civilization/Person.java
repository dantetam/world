package io.github.dantetam.world.civilization;

import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.items.Inventory;

public class Person {

	private int id;
	public LocalTile location;
	public String name;

	// Maps item id to item objects, for finding out quickly if this person has item x
	public Inventory inventory;

	public Person(String name) {
		this.name = name;
		inventory = new Inventory();
	}

	public boolean equals(Object other) {
		if (!(other instanceof Person)) {
			return false;
		}
		Person person = (Person) other;
		return this.id == person.id;
	}

}
