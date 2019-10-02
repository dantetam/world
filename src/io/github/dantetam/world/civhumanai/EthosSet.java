  package io.github.dantetam.world.civhumanai;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.dantetam.toolbox.CollectionUtil;
import io.github.dantetam.toolbox.Pair;
import io.github.dantetam.world.process.LocalProcess;

public class EthosSet {

	//Relating to this person's general ethics and attitudes towards everyday decisions
	public Map<String, Ethos> greatEthos;
	public Map<String, Ethos> ethosPersonalityTraits; //personality traits parse and effect on rel., etc.
	public Map<String, Ethos> ethosEconomics; //For certain inherent valuings or ideas of resources
	
	//Includes attitudes towards crafting, and human-human/human-item interactions
	public Map<LocalProcess, Ethos> ethosTowardsProcesses;
	
	//Includes attitudes towards personality traits
	//only major ethos. Do not have an opinion about having an opinion on fish.
	public Map<Ethos, Ethos> ethosTowardsOtherEthos;
	
	public Map<Integer, Ethos> ethosTowardsItems; //Indexed by item id
	
	public Map<String, Ethos> ethosTowardsFreeActions;
	
	//Relating to choice of career and object preferences, like for food
	public Map<String, Ethos> personalBias; 
	
	private Collection<Ethos> allEthosSaved;
	
	public static double getTotalEthosDifference(EthosSet ethosSetA, EthosSet ethosSetB) {
		Map<String, Double> ethosDiff = getDiffEthosMapping(ethosSetA, ethosSetB);
		double sum = 0;
		for (Entry<String, Double> entry: ethosDiff.entrySet()) {
			sum += entry.getValue();
		}
		return sum;
	}
	
	private static Map<String, Double> getDiffEthosMapping(EthosSet ethosSetA, EthosSet ethosSetB) {
		Map<String, Ethos> ethosMapA = ethosSetA.getEthosMapping();
		Map<String, Ethos> ethosMapB = ethosSetB.getEthosMapping();

		Set<String> keysA = ethosMapA.keySet();
		Set<String> keysB = ethosMapB.keySet();
		keysA.retainAll(keysB); //A = A intersection B
		Set<String> sharedEthos = keysA;
		
		Map<String, Double> diffMapping = new HashMap<>();
		
		for (String ethosName: sharedEthos) {
			Ethos ethosA = ethosMapA.get(ethosName);
			Ethos ethosB = ethosMapB.get(ethosName);
			
			double sevA = ethosA.severity;
			double sevB = ethosB.severity;
			double logSevA = Math.signum(sevA) * Math.log(sevA); 
			double logSevB = Math.signum(sevB) * Math.log(sevB);
			double diffScore = Math.abs(logSevA - logSevB) + 0.5;
			
			//Shifted and capped logit for diff. score 
			double logitScore = Math.log(diffScore / (1 - diffScore));
			logitScore = Math.min(5, logitScore);
			
			diffMapping.put(ethosName, logitScore);
		}
		
		return diffMapping;
	}
	
	//Return the ethos that has the closest difference score to the value desiredDiff
	public static Pair<Ethos> getClosestEthosWithDiffVal(EthosSet ethosSetA, EthosSet ethosSetB, 
			double desiredDiff) {
		Map<String, Ethos> ethosMapA = ethosSetA.getEthosMapping();
		Map<String, Ethos> ethosMapB = ethosSetB.getEthosMapping();
		Map<String, Double> ethosDiff = getDiffEthosMapping(ethosSetA, ethosSetB);
		Pair<Ethos> closestDiffEthos = null;
		double bestDiff = 0;
		for (Entry<String, Double> entry: ethosDiff.entrySet()) {
			double diff = entry.getValue();
			if (diff < bestDiff || closestDiffEthos == null) {
				String ethosName = entry.getKey();
				closestDiffEthos = new Pair<>(ethosMapA.get(ethosName), ethosMapB.get(ethosName)); 
				bestDiff = diff;
			}
		}
		return closestDiffEthos;
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
	
	public void initAllEthosSaved() {
		allEthosSaved = CollectionUtil.getColns(
				this.greatEthos.values(),
				this.ethosPersonalityTraits.values(),
				this.ethosTowardsItems.values(),
				this.ethosTowardsProcesses.values()
			);
	}
	
	public Set<Ethos> getAllEthos() {
		if (allEthosSaved == null) {
			initAllEthosSaved();
		}
		return new HashSet<Ethos>(
				allEthosSaved
			);
	}
	
	public Map<String, Ethos> getEthosMapping() {
		Collection<Ethos> allEthos = this.getAllEthos();
		Map<String, Ethos> ethosMap = new HashMap<>();
		for (Ethos ethos: allEthos) {
			ethosMap.put(ethos.name, ethos);
		}
		return ethosMap;
	}
	
}
