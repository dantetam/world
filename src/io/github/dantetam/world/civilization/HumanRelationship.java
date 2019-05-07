package io.github.dantetam.world.civilization;

import java.util.List;
import java.util.Map.Entry;

import io.github.dantetam.world.civilization.HumanBrain.Ethos;

public class HumanRelationship {

	public LivingEntity beingA;
	public LivingEntity beingB;
	
	public List<LocalExperience> sharedExperiences;
	
	public double opinionA, opinionB;
	
	public boolean equals(Object other) {
		if (!(other instanceof HumanRelationship)) {
			return false;
		}
		HumanRelationship rel = (HumanRelationship) other;
		return (beingA.equals(rel.beingA) && beingB.equals(rel.beingB)) || 
				(beingA.equals(rel.beingB) && beingB.equals(rel.beingA));
	}
	
	public int hashCode() {
		return beingA.hashCode() + beingB.hashCode();
	}
	
}
