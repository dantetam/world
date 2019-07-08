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
		NEUTRAL, FAMILY, MARRIAGE, FRIEND 
		//TODO: Initialize, implement, set to correct data field 
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
		emotionGamut = new EmotionGamut(); 
		double opinionSum = 0;
		
		//Personality traits effect on human relationships
		for (Ethos ethos: human.brain.ethosSet.getAllEthos()) {
			if (ethos.name.equals("Kind")) {
				emotionGamut.addEmotion("Kindness", 20.0);
				emotionGamut.addEmotion("Honor", 10.0);
			}
			else if (ethos.name.equals("Mean")) {
				emotionGamut.addEmotion("Hate", 20.0);
				emotionGamut.addEmotion("Honor", -10.0);
			}
			else if (ethos.name.equals("Pacifistic")) {
				emotionGamut.addEmotion("Honor", 10.0);
				emotionGamut.addEmotion("Rationality", 10.0);
			}
			else if (ethos.name.equals("Honest")) {
				emotionGamut.addEmotion("Honor", 25.0);
			}
			else if (ethos.name.equals("Dishonest")) {
				emotionGamut.addEmotion("Honor", -25.0);
				emotionGamut.addEmotion("Rationality", 5.0);
			}
		}
		
		//This is not intended as a cultural critique on real world sexuality,
		//nor is it even an accurate modeling.
		double baseAttraction = 10;
		String attractGender = human.dna.getDnaMapping("sexualOri");
		String otherPersonGender = human.dna.getDnaMapping("gender");
		String otherPersonSex = human.dna.getDnaMapping("sex");
		if (attractGender == "all") {
			baseAttraction += 20;
		}
		else if (attractGender == otherPersonGender && attractGender == otherPersonSex) {
			baseAttraction += 30;
		}
		else if (attractGender == otherPersonGender) {
			baseAttraction += 10;
		}
		else {
			baseAttraction -= 30;
		}
		emotionGamut.addEmotion("Attraction", baseAttraction);
		
		for (Ethos ethos: human.brain.ethosSet.getAllEthos()) {
			if (ethos.name.equals("Greedy") || ethos.name.equals("Ambitious")) {
				double diffWealth = targetHuman.getTotalWealth() / human.getTotalWealth();
				if (diffWealth > 0.75) {
					double severityMulti = ethos.getLogisticVal(0, 2.5);
					double feeling = severityMulti * diffWealth * 5;
					emotionGamut.addEmotion("Hate", feeling);
					emotionGamut.addEmotion("Admiration", feeling / 2);
				}
				emotionGamut.addEmotion("Rationality", 10.0);
			}
			else if (ethos.name.equals("Charitable")) {
				double diffWealth = human.getTotalWealth() / targetHuman.getTotalWealth();
				if (diffWealth > 0.75) {
					double severityMulti = ethos.getLogisticVal(0, 2.5);
					double feeling = severityMulti * diffWealth * 5;
					emotionGamut.addEmotion("Kindness", feeling);
					emotionGamut.addEmotion("Admiration", feeling / 3);
				}
			}
			
			else if (ethos.name.equals("Gluttonous")) {
				
			}
			
			else if (ethos.name.equals("Obedient")) {
				//Power and prestige measurements for this ethos
				double diffWealth = human.getTotalPowerPrestige() / targetHuman.getTotalPowerPrestige();
				if (diffWealth > 0.75) {
					double severityMulti = ethos.getLogisticVal(0, 2.5);
					double feeling = severityMulti * diffWealth * 5;
					emotionGamut.addEmotion("Admiration", feeling / 3);
				}
			}
			else if (ethos.name.equals("Defiant")) {
				//Power and prestige measurements for this ethos
				double diffWealth = human.getTotalPowerPrestige() / targetHuman.getTotalPowerPrestige();
				if (diffWealth > 1.25) {
					double severityMulti = ethos.getLogisticVal(0, 2.5);
					double feeling = severityMulti * (diffWealth - 1) * 8;
					emotionGamut.addEmotion("Admiration", - feeling / 3);
				}
			}
						
			else if (ethos.name.equals("Ethnocentrism")) {
				double severityMulti = ethos.getLogisticVal(0, 2.5);
				double raceSimilarity = human.dna.compareGenesDist(targetHuman.dna, "race");
				double culSimilarityEmbed = human.dna.compareGenesDist(targetHuman.dna, "culture");
				//double culSimilarityApparent = human.brain.greatEthos
				double ethnoDiffUtil = severityMulti * (raceSimilarity + culSimilarityEmbed * 0.5 - 0.9) * 10;
				if (ethnoDiffUtil > 0) {
					emotionGamut.addEmotion("Kindness", ethnoDiffUtil);
					emotionGamut.addEmotion("Hate", -ethnoDiffUtil);
				}
				emotionGamut.addEmotion("Rationality", -5.0);
			}
			else if (ethos.name.equals("Open") || ethos.name.equals("Curious")) {
				emotionGamut.addEmotion("Rationality", 10.0);
			}
			else if (ethos.name.equals("Closed") || ethos.name.equals("Racist") || ethos.name.equals("Belligerent")) {
				double severityMulti = ethos.getLogisticVal(-2.5, 2.5);
				double totalEthosDiff = EthosSet.getTotalEthosDifference(human.brain.ethosSet, targetHuman.brain.ethosSet);
				double feeling = severityMulti * totalEthosDiff * 10;
				emotionGamut.addEmotion("Hate", feeling);
				emotionGamut.addEmotion("Rationality", -10.0);
			}
		}
		for (LocalExperience experience: this.sharedExperiences) {
			opinionSum += experience.opinion;
		}
		
		//Find dot product of this vector with current emotion range of person 
		//to determine opinion sum.
		Map<String, Double> weights = human.brain.feelingGamutWeights;
		return emotionGamut.dotProductWeights(weights);
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
