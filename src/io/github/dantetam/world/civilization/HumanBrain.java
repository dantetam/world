package io.github.dantetam.world.civilization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.analysis.function.Sigmoid;

/**
 * For all civilized and sentient creatures that can feel coherent moral thoughts,
 * and opinions towards foreigners, other people in their household, society, etc.
 * @author Dante
 *
 */

public class HumanBrain {

	//Relating to this person's general ethics and attitudes towards everyday decisions
	public Map<String, Ethos> personalEthos; 
	
	//Relating to choice of career and object preferences, like for food
	public Map<String, Ethos> personalBias; 
	
	public HumanBrain() {
		personalEthos = new HashMap<>();
		personalBias = new HashMap<>();
	}
	
	public static double getEthosDifference(HumanBrain brainA, HumanBrain brainB) {
		double difference = 0;
		Set<String> keysA = new HashSet<>(brainA.personalEthos.keySet());
		Set<String> keysB = new HashSet<>(brainB.personalEthos.keySet());
		keysA.retainAll(keysB);
		Set<String> sharedKeys = keysA;
		for (String sharedKey: sharedKeys) {
			double logSevA = Math.log(brainA.personalEthos.get(sharedKey).severity); 
			double logSevB = Math.log(brainB.personalEthos.get(sharedKey).severity);
			double diffScore = Math.abs(logSevA - logSevB) + 0.5;
			
			//Shifted and capped logit for diff. score 
			double logitScore = Math.log(diffScore / (1 - diffScore));
			logitScore = Math.min(5, logitScore);
			difference += logitScore;
		}
		return difference;
	}
	
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
		
		public double severity; 
		
		public double getLogisticVal(double low, double high) {
			return new Sigmoid(low, high).value(severity);
		}
		public double getNormLogisticVal() {
			return new Sigmoid().value(severity);
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
