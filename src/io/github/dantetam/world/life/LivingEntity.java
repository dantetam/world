package io.github.dantetam.world.life;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.dataparse.AnatomyData.Body;
import io.github.dantetam.world.grid.LocalBuilding;
import io.github.dantetam.world.grid.LocalTile;
import io.github.dantetam.world.items.Inventory;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.LocalJob;
import io.github.dantetam.world.process.LocalProcess;
import io.github.dantetam.world.process.LocalProcess.ProcessStep;
import io.github.dantetam.world.process.priority.Priority;
import io.github.dantetam.world.process.prioritytask.Task;

public abstract class LivingEntity {

	private int id;
	public LocalTile location;
	public String name;

	//A cloned Process object which keeps track of the remaining steps in the process cycle
	public LocalProcess processProgress;
	public Priority activePriority;
	public List<Task> currentQueueTasks;
	public LocalJob jobProcessProgress; //For activities mandated by a lord, employer, or society
	
	public LocalBuilding processBuilding;
	public LocalTile processTile; //Person should move directly to this tile
	public LocalTile targetTile; //Person should target this tile for harvest, operations, etc.
	
	//Link to all items owned by this person, that can be stored in and out of the inventory,
	//or in the game world.
	public List<LocalBuilding> ownedBuildings;
	public List<InventoryItem> ownedItems; //Note, this is different from the inventory	
	
	// Maps item id to item objects, for finding out quickly if this person has item x
	public Inventory inventory;
	
	public DNALivingEntity dna;
	
	protected static final double NUTRITION_CONSTANT = 10;
	protected static final double REST_CONSTANT_TICK = 100 / (6 * 60);
	protected static final double NUTRI_CONST_LOSS_TICK = 100 / (24 * 60);
	protected static final double LIVE_CONST_LOSS_TICK = 100 / (18 * 60);
	public double nutrition, maxNutrition, rest, maxRest, fun, maxFun; //hydration, maxHydration, 
	
	public double age;
	
	//Includes everything wearable, such as normal shirts, and combat armor
	public Body body;
	
	public LivingEntity(String name) {
		this.name = name;
		inventory = new Inventory();
		ownedBuildings = new ArrayList<>();
		ownedItems = new ArrayList<>();
		age = 0;
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
	
	public double getTotalPowerPrestige() {
		TODO //Implement land claims at society level
		return getTotalWealth();
	}
	
	public double getTotalWealth() {
		double sumWealth = 0;
		sumWealth += inventory.getTotalWealth();
		
		double ownedItemsWealth = 0;
		for (InventoryItem item: this.ownedItems) {
			double wealth = ItemData.getBaseItemValue(item.itemId);
			ownedItemsWealth += wealth * item.quantity;
		}
		sumWealth += ownedItemsWealth;
		
		double ownedBuildingsWealth = 0;
		for (LocalBuilding building: ownedBuildings) {
			double wealth = ItemData.getBaseItemValue(building.buildingId);
			ownedBuildingsWealth += wealth;
			ownedBuildingsWealth += building.inventory.getTotalWealth();
		}
		sumWealth += ownedBuildingsWealth;
		
		return sumWealth;
	}
	
	public void wearItem(InventoryItem item) {
		TODO
	}

}
