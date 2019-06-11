package io.github.dantetam.world.civhumanai;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.world.process.LocalProcess;

public class EthosSet {

	//Relating to this person's general ethics and attitudes towards everyday decisions
	public Map<String, Ethos> greatEthos;
	public Map<String, Ethos> ethosPersonalityTraits;
	public Map<String, Ethos> ethosEconomics; //For certain inherent valuings or ideas of resources
	
	//Includes attitudes towards crafting, and human-human/human-item interactions
	public Map<LocalProcess, Ethos> ethosTowardsProcesses;
	
	//Includes attitudes towards personality traits
	//only major ethos. Do not have an opinion about having an opinion on fish.
	public Map<Ethos, Ethos> ethosTowardsOtherEthos;
	
	public Map<Integer, Ethos> ethosTowardsItems; //Indexed by item id
	
	//Relating to choice of career and object preferences, like for food
	public Map<String, Ethos> personalBias; 
	
	public static double getEthosDifference(EthosSet ethosA, EthosSet ethosB) {
		double difference = 0;
		Set<String> keysA = new HashSet<>(ethosA.greatEthos.keySet());
		Set<String> keysB = new HashSet<>(ethosB.greatEthos.keySet());
		keysA.retainAll(keysB); //A = A intersection B
		Set<String> sharedKeys = keysA;
		for (String sharedKey: sharedKeys) {
			double sevA = ethosA.greatEthos.get(sharedKey).severity;
			double sevB = ethosB.greatEthos.get(sharedKey).severity;
			double logSevA = Math.signum(sevA) * Math.log(sevA); 
			double logSevB = Math.signum(sevB) * Math.log(sevB);
			double diffScore = Math.abs(logSevA - logSevB) + 0.5;
			
			//Shifted and capped logit for diff. score 
			double logitScore = Math.log(diffScore / (1 - diffScore));
			logitScore = Math.min(5, logitScore);
			difference += logitScore;
		}
		return difference;
	}
	
	public EthosSet() {
		greatEthos = new HashMap<>();
		personalBias = new HashMap<>();
		ethosPersonalityTraits = new HashMap<>();
		ethosEconomics = new HashMap<>();
		ethosTowardsProcesses = new HashMap<>();
		ethosTowardsOtherEthos = new HashMap<>();
		ethosTowardsItems = new HashMap<>();
	}
	
}
