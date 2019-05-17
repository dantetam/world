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

public class HumanHumanRel extends HumanRelationship {

	public Human human, targetHuman;
	public boolean isPersonalHostileOneWay; //Note, this is only for personal disputes and fights, not for wars between societies
	public String relationshipType;
	
	public HumanHumanRel(Human human, Human targetHuman, String relationshipType) {
		super();
		this.human = human;
		this.targetHuman = targetHuman;
		isPersonalHostileOneWay = false;
		this.relationshipType = relationshipType;
	}
	
	@Override
	public double reevaluateOpinion(Date date) {
		double ethosDiff = HumanBrain.getEthosDifference(human.brain, targetHuman.brain);
		double opinionSum = 0;
		for (Entry<String, Ethos> entry: human.brain.greatEthos.entrySet()) {
			Ethos ethos = entry.getValue();
			if (ethos.name.equals("Language Ethoscentrism")) {
				
			}
		}
		for (LocalExperience experience: this.sharedExperiences) {
			opinionSum += experience.opinion;
			//TODO
		}
		return opinionSum;
	}

	public boolean equals(Object other) {
		if (!(other instanceof HumanHumanRel)) {
			return false;
		}
		HumanHumanRel rel = (HumanHumanRel) other;
		return (human.equals(rel.human) && targetHuman.equals(rel.targetHuman)) || 
				(human.equals(rel.targetHuman) && targetHuman.equals(rel.human));
	}
	
	public int hashCode() {
		return human.hashCode() + targetHuman.hashCode();
	}
	
}
