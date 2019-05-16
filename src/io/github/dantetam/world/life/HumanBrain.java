package io.github.dantetam.world.life;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.analysis.function.Sigmoid;

import io.github.dantetam.world.civhumanrelation.HumanHumanRel;
import io.github.dantetam.world.civhumanrelation.HumanRelationship;
import io.github.dantetam.world.dataparse.EthosData;
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
	public Map<String, Ethos> greatEthos;
	public Map<String, Ethos> ethosPersonalityTraits;
	
	//Includes attitudes towards crafting, and human-human/human-item interactions
	public Map<LocalProcess, Ethos> ethosTowardsProcesses;
	
	//Includes attitudes towards personality traits
	//only major ethos. Do not have an opinion about having an opinion on fish.
	public Map<Ethos, Ethos> ethosTowardsOtherEthos;
	
	public Map<Integer, Ethos> ethosTowardsItems; //Indexed by item id
	
	//Relating to choice of career and object preferences, like for food
	public Map<String, Ethos> personalBias; 
	
	public Map<Human, HumanRelationship> indexedRelationships;
	
	public HumanBrain(Human host) {
		this.host = host;
		
		greatEthos = new HashMap<>();
		personalBias = new HashMap<>();
		ethosPersonalityTraits = new HashMap<>();
		ethosTowardsProcesses = new HashMap<>();
		ethosTowardsOtherEthos = new HashMap<>();
		ethosTowardsItems = new HashMap<>();
		
		indexedRelationships = new HashMap<>();
	}
	
	public static double getEthosDifference(HumanBrain brainA, HumanBrain brainB) {
		double difference = 0;
		Set<String> keysA = new HashSet<>(brainA.greatEthos.keySet());
		Set<String> keysB = new HashSet<>(brainB.greatEthos.keySet());
		keysA.retainAll(keysB);
		Set<String> sharedKeys = keysA;
		for (String sharedKey: sharedKeys) {
			double logSevA = Math.log(brainA.greatEthos.get(sharedKey).severity); 
			double logSevB = Math.log(brainB.greatEthos.get(sharedKey).severity);
			double diffScore = Math.abs(logSevA - logSevB) + 0.5;
			
			//Shifted and capped logit for diff. score 
			double logitScore = Math.log(diffScore / (1 - diffScore));
			logitScore = Math.min(5, logitScore);
			difference += logitScore;
		}
		return difference;
	}
	
	public void addHumanRel(Human target, String relType) { 
		indexedRelationships.put(target, new HumanHumanRel(this.host, target, relType));
	}
	
	/**
	 * Represents a societal thought, personality trait, or other intellectual quirk
	 * Note that ethos are essentially brain values with state, so they should be cloned
	 * into other objects that require ethos.
	 * @author Dante
	 *
	 */
	public static class Ethos {
		public String name;
		
		//Compilable in the standard format of people opinion modifier syntax
		//All of these are evaluated at once, i.e.:
		
		/*
		 * For all other people in society,
		 * if person is an elf and has a job,
		 * lower base opinion to -50.
		 * 
		 * {"SOCIETY_OTHERS", "IF_RACE_IS_ELF", "BASE_OPINION"}
		 */
		public EthosModifier[] modifiers; 
		
		/*
		 * After believing in this ethos, apply these psychological effects to the human
		 * or sentient being when making decisions. The effects:
		 * 
		 */
		public EthosModifier[] effects;
		
		public double severity; 
		public double ethosLifetimeHappiness;
		
		public Ethos(String name, double severity, String modsString, String effectsString) {
			this.name = name;
			this.severity = severity;
			this.modifiers = EthosData.parseEthosMods(modsString);
			this.effects = EthosData.parseEthosMods(effectsString);
			this.ethosLifetimeHappiness = 0;
		}
		
		public double getLogisticVal(double low, double high) {
			return new Sigmoid(low, high).value(severity);
		}
		public double getNormLogisticVal() {
			return new Sigmoid().value(severity);
		}
		
		public Ethos clone() {
			Ethos clone = new Ethos(this.name, this.severity, "", "");
			clone.modifiers = this.modifiers;
			clone.effects = this.effects;
			return clone;
		}
	}
	
	public static class EthosModifier {
		public String name;
		public double value;
		public EthosModifier(String name, double value) {
			this.name = name;
			this.value = value;
		}
	}
	
}
