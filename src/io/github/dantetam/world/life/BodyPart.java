package io.github.dantetam.world.life;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import io.github.dantetam.world.items.CombatItem;
import io.github.dantetam.world.life.AnatomyData.BodyDamage;

public class BodyPart {
	public String name;
	public Vector3f position;
	public boolean isMainBodyPart;
	public double size;
	public double vulnerability; //In terms of combat, the chance this part is hit (normalized)
	public double health, maxHealth;
	public double dexterity; //The ability of this limb to hold and manuever items
			//This stat is to discourage people from holding weapons with their mouths,
			//and also provide bonuses to non-human creatures and sentient beings.
	public List<BodyDamage> damages; //The current total rate of damage,
			//Blood loss, disease, and corruption that affects the whole body
	
	public List<CombatItem> heldItems;
	public int heldItemWeightCapLeft = 1;
	public int originalWeightCap = 1;
	
	//Parent-child relationship for some body parts, which define a strict iff existence
	//of the use of these two fields.
	public List<BodyPart> neighboringParts;
	public List<BodyPart> insideParts;
	public BodyPart bodyPartParent;
	
	public BodyPart(String name, Vector3f position, boolean isMainBodyPart, double size, 
			double vulnerability, double maxHealth, double dexterity) {
		this.name = name;
		this.position = position;
		this.size = size;
		this.vulnerability = vulnerability;
		this.maxHealth = maxHealth;
		this.health = maxHealth;
		this.dexterity = dexterity;
		damages = new ArrayList<>();
		heldItems = new ArrayList<>();
		neighboringParts = new ArrayList<>();
		insideParts = new ArrayList<>();
		bodyPartParent = null;
	}
	
	public boolean hasCapacity() {
		return heldItemWeightCapLeft > 0;
	}
	
	/**
	 * Wear the new item, while removing the first items until this body part is at, or under capacity
	 * @return 
	 */
	public List<CombatItem> wearCombatItem(CombatItem item) {
		List<CombatItem> itemsNotWorn = new ArrayList<>();
		if (originalWeightCap >= item.itemWeight) {
			heldItems.add(item);
			while (!hasCapacity()) {
				CombatItem removedItem = heldItems.remove(0);
				itemsNotWorn.add(removedItem);
				heldItemWeightCapLeft -= removedItem.itemWeight;
			}
		}
		else {
			itemsNotWorn.add(item);
		}
		return itemsNotWorn;
	}
	
	public void removeCombatItem(CombatItem item) {
		if (heldItems.contains(item)) {
			heldItems.remove(item);
			heldItemWeightCapLeft -= item.itemWeight;
		}
	}
	
	public double getDamageValue() {
		double sum = 0;
		for (BodyDamage damage: damages) {
			sum += damage.damage;
		}
		return sum;
	}
	
	public void addAdjacentPart(BodyPart part) {
		this.neighboringParts.add(part);
		part.neighboringParts.add(this);
	}
	
	public BodyPart chainPartInside(BodyPart part) {
		insideParts.add(part);
		part.bodyPartParent = this;
		return this;
	}
	
	public void removePartInside(BodyPart part) {
		if (insideParts.contains(part)) {
			insideParts.remove(part);
		}
		part.bodyPartParent = null;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof BodyPart)) {
			return false;
		}
		BodyPart bodyPart = (BodyPart) other;
		return this.name.equals(bodyPart.name);
	}
	
	public int hashCode() {
		return this.name.hashCode();
	}
}