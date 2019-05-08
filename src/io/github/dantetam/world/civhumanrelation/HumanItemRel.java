package io.github.dantetam.world.civhumanrelation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import io.github.dantetam.world.civilization.Human;
import io.github.dantetam.world.civilization.HumanBrain;
import io.github.dantetam.world.civilization.LivingEntity;
import io.github.dantetam.world.civilization.LocalExperience;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.civilization.HumanBrain.Ethos;
import io.github.dantetam.world.items.InventoryItem;

public class HumanItemRel extends HumanRelationship {

	public Human human;
	public InventoryItem item;
	
	public HumanItemRel(Human human, InventoryItem item) {
		this.human = human;
		this.item = item;
		sharedExperiences = new ArrayList<>();
		opinion = 0;
	}
	
	@Override
	public double reevaluateOpinion(Date date) {
		return 0;
	}

	public boolean equals(Object other) {
		if (!(other instanceof HumanItemRel)) {
			return false;
		}
		HumanItemRel rel = (HumanItemRel) other;
		return human.equals(rel.human) && item.equals(rel.item);
	}
	
	public int hashCode() {
		return human.hashCode() + item.hashCode();
	}
	
}
