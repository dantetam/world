package io.github.dantetam.world.life;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.world.civhumanai.EthosSet;
import io.github.dantetam.world.civhumanrelation.EmotionGamut;
import io.github.dantetam.world.civhumanrelation.HumanHumanRel;
import io.github.dantetam.world.civhumanrelation.HumanHumanRel.HumanHumanRelType;
import io.github.dantetam.world.civhumanrelation.HumanRelationship;
import io.github.dantetam.world.civhumanrelation.HumanSocietyRel;
import io.github.dantetam.world.civilization.LocalExperience;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.process.LocalProcess;

/**
 * For all civilized and sentient creatures that can feel coherent moral thoughts,
 * and opinions towards foreigners, other people in their household, society, etc.
 * @author Dante
 *
 */

public class HumanBrain {

	public Human host;
	
	//Relating to this person's general ethics and attitudes towards everyday decisions
	public EthosSet ethosSet;
	
	public List<LocalExperience> experiences;
	
	public Map<Human, HumanHumanRel> indexedHumanRelationships;
	public Map<Society, HumanSocietyRel> indexedSocRelationships;
	public Map<String, Double> languageCodesStrength;
	
	public EmotionGamut feelingGamutWeights;
	
	public HumanBrain(Human host) {
		this.host = host;
		this.ethosSet = new EthosSet();
		
		indexedHumanRelationships = new HashMap<>();
		indexedSocRelationships = new HashMap<>();
		
		languageCodesStrength = new HashMap<>();
		
		experiences = new ArrayList<>();
		
		initFeelingWeights();
	}
	
	public void addHumanRel(Human target) { 
		addHumanRel(target, HumanHumanRelType.NEUTRAL);
	}
	public void addHumanRel(Human target, HumanHumanRelType relType) { 
		indexedHumanRelationships.put(target, new HumanHumanRel(this.host, target, relType));
	}
	public HumanHumanRel getHumanRel(Human target) {
		if (indexedHumanRelationships.containsKey(target)) {
			return (HumanHumanRel) indexedHumanRelationships.get(target);
		}
		return null;
	}
	
	public void addSocRel(Society target) { 
		indexedSocRelationships.put(target, new HumanSocietyRel(this.host, target));
	}
	public HumanSocietyRel getSocRel(Society target) {
		if (indexedHumanRelationships.containsKey(target)) {
			return indexedSocRelationships.get(target);
		}
		return null;
	}
	
	//TODO: Use these values to simulate non-human ethos i.e. sentient non-human feelings towards others in social situations?
	private void initFeelingWeights() {
		feelingGamutWeights = new EmotionGamut();
		feelingGamutWeights.addEmotion("Kindness", 1.5);
		feelingGamutWeights.addEmotion("Admiration", 0.3);
		feelingGamutWeights.addEmotion("Honor", 0.3);
		feelingGamutWeights.addEmotion("Hate", -1.5);
	}
	
	TODO
	//Factor in local experiences into ethos, debates, chats (reminiscing and telling others memories), and so on
	//Use their weights as well
	
	//Have more things impacted by human relations: ability to influence/learn ethos, probabilities to do activities like chat and fight, and so on
	
	
}
