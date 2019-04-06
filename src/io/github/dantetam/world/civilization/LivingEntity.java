package io.github.dantetam.world.civilization;

import java.util.List;

import io.github.dantetam.world.grid.LocalBuilding;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.Process;
import io.github.dantetam.world.process.Process.ProcessStep;
import io.github.dantetam.world.process.priority.Priority;
import io.github.dantetam.world.process.prioritytask.Task;

public class LivingEntity {

	private int id;
	public LocalTile location;
	public String name;

	//A cloned Process object which keeps track of the remaining steps in the process cycle
	public Process processProgress;
	public Priority activePriority;
	public List<Task> currentQueueTasks;
	
	public LocalBuilding processBuilding;
	
	//Link to all items owned by this person, that can be stored in and out of the inventory,
	//or in the game world.
	public List<LocalBuilding> ownedBuildings;
	public List<InventoryItem> ownedItems; //Note, this is different from the inventory	
	
	// Maps item id to item objects, for finding out quickly if this person has item x
	public Inventory inventory;

	public LivingEntity(String name) {
		this.name = name;
		inventory = new Inventory();
	}

	public boolean equals(Object other) {
		if (!(other instanceof LivingEntity)) {
			return false;
		}
		LivingEntity person = (LivingEntity) other;
		return this.id == person.id;
	}

}
