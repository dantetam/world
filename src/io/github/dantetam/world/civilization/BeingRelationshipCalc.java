package io.github.dantetam.world.civilization;

import java.util.Map.Entry;

import io.github.dantetam.world.civilization.HumanBrain.Ethos;
import io.github.dantetam.world.items.InventoryItem;

public class BeingRelationshipCalc {

	TODO
	
	public static double reevaluateOpinion(LivingEntity beingA, LivingEntity beingB) {
		if (beingA instanceof Human && beingB instanceof Human) {
			Human humanA = (Human) beingA;
			Human humanB = (Human) beingB;
			double ethosDiff = HumanBrain.getEthosDifference(humanA.brain, humanB.brain);
			for (Entry<String, Ethos> entry: humanA.brain.personalEthos.entrySet()) {
				Ethos ethos = entry.getValue();
				
			}
		}
		return 0;
	}
	
	public static double reevaluateOpinion(LivingEntity being, InventoryItem item) {
		
	}
	
	
}
