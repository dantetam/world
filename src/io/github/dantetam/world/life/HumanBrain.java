package io.github.dantetam.world.life;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.world.civhumanai.EthosSet;
import io.github.dantetam.world.civhumanrelation.HumanHumanRel;
import io.github.dantetam.world.civhumanrelation.HumanHumanRel.HumanHumanRelType;
import io.github.dantetam.world.civhumanrelation.HumanRelationship;
import io.github.dantetam.world.civilization.LocalExperience;
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
	
	public Map<Human, HumanRelationship> indexedRelationships;
	public Map<String, Double> languageCodesStrength;
	
	public Map<String, Double> feelingGamutWeights;
	
	public HumanBrain(Human host) {
		this.host = host;
		this.ethosSet = new EthosSet();
		
		indexedRelationships = new HashMap<>();
		languageCodesStrength = new HashMap<>();
		
		experiences = new ArrayList<>();
		
		initFeelingWeights();
	}
	
	public void addHumanRel(Human target) { 
		addHumanRel(target, HumanHumanRelType.NEUTRAL);
	}
	public void addHumanRel(Human target, HumanHumanRelType relType) { 
		indexedRelationships.put(target, new HumanHumanRel(this.host, target, relType));
	}
	
	public HumanHumanRel getHumanRel(Human target) {
		if (indexedRelationships.containsKey(target)) {
			return (HumanHumanRel) indexedRelationships.get(target);
		}
		return null;
	}
	
	private void initFeelingWeights() {
		feelingGamutWeights = new HashMap<>();
		feelingGamutWeights.put("Kindness", 1.5);
		feelingGamutWeights.put("Admiration", 0.3);
		feelingGamutWeights.put("Honor", 0.3);
		feelingGamutWeights.put("Hate", -1.5);
	}
	
}
