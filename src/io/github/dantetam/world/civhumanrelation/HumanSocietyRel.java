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

public class HumanSocietyRel extends HumanRelationship {

	public Human human;
	public Society society;
	
	public HumanSocietyRel(Human human, Society society) {
		this.human = human;
		this.society = society;
		sharedExperiences = new ArrayList<>();
		opinion = 0;
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
