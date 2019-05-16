package io.github.dantetam.world.civhumanrelation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import io.github.dantetam.world.civilization.LocalExperience;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.HumanBrain;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.life.HumanBrain.Ethos;

public class HumanItemRel extends HumanRelationship {

	public Human human;
	public InventoryItem item;
	
	public HumanItemRel(Human human, InventoryItem item) {
		super();
		this.human = human;
		this.item = item;
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
