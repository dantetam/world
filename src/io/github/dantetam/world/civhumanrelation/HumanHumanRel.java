package io.github.dantetam.world.civhumanrelation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import io.github.dantetam.world.civhumanai.Ethos;
import io.github.dantetam.world.civhumanai.EthosSet;
import io.github.dantetam.world.civilization.LocalExperience;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.HumanBrain;
import io.github.dantetam.world.life.LivingEntity;

public class HumanHumanRel extends HumanRelationship {

	public Human human, targetHuman;
	public boolean isPersonalHostileOneWay; //Note, this is only for personal disputes and fights, not for wars between societies
	public HumanHumanRelType relationshipType;
	
	public enum HumanHumanRelType {
		NEUTRAL, FAMILY, MARRIAGE, FRIEND //TODO: Initialize, implement, set to correct data field
	}
	
	public HumanHumanRel(Human human, Human targetHuman, HumanHumanRelType relationshipType) {
		super();
		this.human = human;
		this.targetHuman = targetHuman;
		isPersonalHostileOneWay = false;
		this.relationshipType = relationshipType;
	}
	
	@Override
	public double reevaluateOpinion(Date date) {
		double opinionSum = 0;
		for (Entry<String, Ethos> entry: human.brain.ethosSet.greatEthos.entrySet()) {
			Ethos ethos = entry.getValue();
			if (ethos.name.equals("Ethnocentrism")) {
				double severityMulti = ethos.getLogisticVal(0, 2.5);
				double raceSimilarity = human.dna.compareGenesDist(targetHuman.dna, "race");
				double culSimilarityEmbed = human.dna.compareGenesDist(targetHuman.dna, "culture");
				//double culSimilarityApparent = human.brain.greatEthos
				opinionSum += severityMulti * (raceSimilarity + culSimilarityEmbed * 0.5 - 0.9) * 10; 
			}
			if (ethos.name.equals("Open")) {
				double severityMulti = ethos.getLogisticVal(-2.5, 2.5);
				double totalEthosDiff = EthosSet.getEthosDifference(human.brain.ethosSet, targetHuman.brain.ethosSet);
				opinionSum += -1 * severityMulti * totalEthosDiff * 10;
			}
		}
		for (LocalExperience experience: this.sharedExperiences) {
			opinionSum += experience.opinion;
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
