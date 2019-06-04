package io.github.dantetam.world.civhumanrelation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import io.github.dantetam.world.civhumanrelation.SocietySocietyRel.SocietalRelationMode;
import io.github.dantetam.world.civilization.LocalExperience;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.items.InventoryItem;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.life.HumanBrain;
import io.github.dantetam.world.life.LivingEntity;

public class SocietySocietyRel extends HumanRelationship {

	public Society hostSociety;
	public Society otherSociety;
	
	public SocietalRelationMode societyRelMode; //War, Peace, Def. Allies, Allies, etc.
	
	public enum SocietalRelationMode {
		NEUTRAL, WAR, DEFENSIVE_ALLIES, ALLIES,
		OVERLORD, VASSAL //one-way relations towards, i.e. hostSociety is the overlord of otherSociety
		//A society can only have one overlord
	}
	
	public SocietySocietyRel(Society hostSociety, Society otherSociety) {
		super();
		this.hostSociety = hostSociety;
		this.otherSociety = otherSociety;
		societyRelMode = SocietalRelationMode.NEUTRAL;
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
