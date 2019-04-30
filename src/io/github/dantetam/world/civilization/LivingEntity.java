package io.github.dantetam.world.civilization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.world.dataparse.AnatomyData.Body;
import io.github.dantetam.world.grid.LocalBuilding;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.Process;
import io.github.dantetam.world.process.Process.ProcessStep;
import io.github.dantetam.world.process.priority.Priority;
import io.github.dantetam.world.process.prioritytask.Task;

public abstract class LivingEntity {

	private int id;
	public LocalTile location;
	public String name;

	//A cloned Process object which keeps track of the remaining steps in the process cycle
	public Process processProgress;
	public Priority activePriority;
	public List<Task> currentQueueTasks;
	
	public LocalBuilding processBuilding;
	public LocalTile processTile;
	
	//Link to all items owned by this person, that can be stored in and out of the inventory,
	//or in the game world.
	public List<LocalBuilding> ownedBuildings;
	public List<InventoryItem> ownedItems; //Note, this is different from the inventory	
	
	// Maps item id to item objects, for finding out quickly if this person has item x
	public Inventory inventory;

	protected static final double NUTRITION_CONSTANT = 10;
	protected static final double REST_CONSTANT_TICK = 100 / (6 * 60);
	protected static final double NUTRI_CONST_LOSS_TICK = 100 / (24 * 60);
	protected static final double LIVE_CONST_LOSS_TICK = 100 / (18 * 60);
	public double nutrition, maxNutrition, rest, maxRest; //hydration, maxHydration, 
	
	//Includes everything wearable, such as normal shirts, and combat armor
	public Body body;
	
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
	
	public void feed(double standardUnitNutrition) {
		nutrition = Math.min(nutrition + standardUnitNutrition*NUTRITION_CONSTANT, 
				maxNutrition);
	}
	
	public void rest(double standardRestUnit) {
		rest = Math.min(rest + standardRestUnit*REST_CONSTANT_TICK, maxRest);
	}
	
	public void spendNutrition() {
		nutrition = Math.max(nutrition - NUTRI_CONST_LOSS_TICK, 0);
	}
	
	public void spendEnergy() {
		rest = Math.max(rest - LIVE_CONST_LOSS_TICK, 0);
	}

}
