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

public class HumanSocietyRel extends HumanRelationship {

	public Human human;
	public Society society;
	
	public HumanSocietyRel(Human human, Society society) {
		super();
		this.human = human;
		this.society = society;
	}
	
	@Override
	public double reevaluateOpinion(Date date) {
		return 0;
	}

	public boolean equals(Object other) {
		if (!(other instanceof HumanSocietyRel)) {
			return false;
		}
		HumanSocietyRel rel = (HumanSocietyRel) other;
		return human.equals(rel.human) && society.equals(rel.society);
	}
	
	public int hashCode() {
		return human.hashCode() + society.hashCode();
	}
	
}
