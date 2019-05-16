package io.github.dantetam.world.civhumanrelation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import io.github.dantetam.world.civilization.LocalExperience;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.HumanBrain;
import io.github.dantetam.world.life.LivingEntity;
import io.github.dantetam.world.life.HumanBrain.Ethos;

public class SocietySocietyRel extends HumanRelationship {

	public Society hostSociety;
	public Society otherSociety;
	
	public SocietySocietyRel(Society hostSociety, Society otherSociety) {
		super();
		this.hostSociety = hostSociety;
		this.otherSociety = otherSociety;
	}
	
	@Override
	public double reevaluateOpinion(Date date) {
		return 0;
	}

	public boolean equals(Object other) {
		if (!(other instanceof SocietySocietyRel)) {
			return false;
		}
		SocietySocietyRel rel = (SocietySocietyRel) other;
		return (hostSociety.equals(rel.hostSociety) && otherSociety.equals(rel.otherSociety)) || 
				(hostSociety.equals(rel.otherSociety) && otherSociety.equals(rel.hostSociety));
	}
	
	public int hashCode() {
		return hostSociety.hashCode() + otherSociety.hashCode();
	}
	
}
