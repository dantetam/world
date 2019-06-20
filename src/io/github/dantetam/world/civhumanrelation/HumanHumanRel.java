package io.github.dantetam.world.civhumanrelation;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.world.civhumanai.Ethos;
import io.github.dantetam.world.civhumanai.EthosSet;
import io.github.dantetam.world.civilization.LocalExperience;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.EthosSetInitialize;
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
		Map<String, Double> emotionGamut = new HashMap<>(); 
		//TODO; //Find dot product of this vector with current emotion range of person 
		//to determine opinion sum.
		
		double opinionSum = 0;
		
		for (Ethos ethos: human.brain.ethosSet.getAllHumanEthos()) {
			if (ethos.name.equals("Kind")) {
				MapUtil.addNumMap(emotionGamut, "Kindness", 20.0);
				MapUtil.addNumMap(emotionGamut, "Honor", 10.0);
			}
			else if (ethos.name.equals("Mean")) {
				MapUtil.addNumMap(emotionGamut, "Hate", 20.0);
				MapUtil.addNumMap(emotionGamut, "Honor", -10.0);
			}
			else if (ethos.name.equals("Pacifistic")) {
				MapUtil.addNumMap(emotionGamut, "Honor", 10.0);
				MapUtil.addNumMap(emotionGamut, "Rationality", 10.0);
			}
			else if (ethos.name.equals("Honest")) {
				MapUtil.addNumMap(emotionGamut, "Honor", 25.0);
			}
			else if (ethos.name.equals("Dishonest")) {
				MapUtil.addNumMap(emotionGamut, "Honor", -25.0);
				MapUtil.addNumMap(emotionGamut, "Rationality", 5.0);
			}
		}
		
		for (Ethos ethos: human.brain.ethosSet.getAllHumanEthos()) {
			if (ethos.name.equals("Greedy") || ethos.name.equals("Ambitious")) {
				double diffWealth = targetHuman.getTotalWealth() / human.getTotalWealth();
				if (diffWealth > 0.75) {
					double severityMulti = ethos.getLogisticVal(0, 2.5);
					double feeling = severityMulti * diffWealth * 5;
					MapUtil.addNumMap(emotionGamut, "Hate", feeling);
					MapUtil.addNumMap(emotionGamut, "Admiration", feeling / 2);
				}
				MapUtil.addNumMap(emotionGamut, "Rationality", 10.0);
			}
			else if (ethos.name.equals("Charitable")) {
				double diffWealth = targetHuman.getTotalWealth() / human.getTotalWealth();
				if (diffWealth > 0.75) {
					double severityMulti = ethos.getLogisticVal(0, 2.5);
					double feeling = severityMulti * diffWealth * 5;
					MapUtil.addNumMap(emotionGamut, "Kindness", feeling);
					MapUtil.addNumMap(emotionGamut, "Admiration", feeling / 3);
				}
			}
			
			else if (ethos.name.equals("Gluttonous")) {
				
			}
			
			else if (ethos.name.equals("Obedient")) {
				//TODO //Implement power and prestige measurements for this ethos
				
			}
			else if (ethos.name.equals("Defiant")) {
				
			}
						
			else if (ethos.name.equals("Ethnocentrism")) {
				double severityMulti = ethos.getLogisticVal(0, 2.5);
				double raceSimilarity = human.dna.compareGenesDist(targetHuman.dna, "race");
				double culSimilarityEmbed = human.dna.compareGenesDist(targetHuman.dna, "culture");
				//double culSimilarityApparent = human.brain.greatEthos
				double ethnoDiffUtil = severityMulti * (raceSimilarity + culSimilarityEmbed * 0.5 - 0.9) * 10;
				opinionSum += ethnoDiffUtil; 
				if (ethnoDiffUtil > 0) {
					MapUtil.addNumMap(emotionGamut, "Kindness", ethnoDiffUtil);
					MapUtil.addNumMap(emotionGamut, "Hate", -ethnoDiffUtil);
				}
				MapUtil.addNumMap(emotionGamut, "Rationality", -5.0);
			}
			else if (ethos.name.equals("Open") || ethos.name.equals("Curious")) {
				MapUtil.addNumMap(emotionGamut, "Rationality", 10.0);
			}
			else if (ethos.name.equals("Closed") || ethos.name.equals("Racist") || ethos.name.equals("Belligerent")) {
				double severityMulti = ethos.getLogisticVal(-2.5, 2.5);
				double totalEthosDiff = EthosSet.getEthosDifference(human.brain.ethosSet, targetHuman.brain.ethosSet);
				double feeling = severityMulti * totalEthosDiff * 10;
				opinionSum -= feeling;
				MapUtil.addNumMap(emotionGamut, "Hate", feeling);
				MapUtil.addNumMap(emotionGamut, "Rationality", -10.0);
			}
			//TODO Implement personality traits effect on human relationships
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
