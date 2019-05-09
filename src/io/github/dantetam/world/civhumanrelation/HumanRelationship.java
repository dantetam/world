package io.github.dantetam.world.civhumanrelation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import io.github.dantetam.world.civilization.Human;
import io.github.dantetam.world.civilization.HumanBrain;
import io.github.dantetam.world.civilization.LivingEntity;
import io.github.dantetam.world.civilization.LocalExperience;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.combat.War;
import io.github.dantetam.world.civilization.HumanBrain.Ethos;
import io.github.dantetam.world.items.InventoryItem;

public abstract class HumanRelationship {
	
	public List<LocalExperience> sharedExperiences;
	public double opinion;
	
	public HumanRelationship() {
		sharedExperiences = new ArrayList<>();
		opinion = 0;
	}
	
	public abstract double reevaluateOpinion(Date date);
	
	public abstract boolean equals(Object other);
	public abstract int hashCode();
	
	public static boolean isHostileTowards(LivingEntity being, LivingEntity target) {
		if (being instanceof Human && target instanceof Human) {
			return isHostileTowardsSentient((Human) being, (Human) target);
		}
		return false;
	}
	
	public static boolean isHostileTowardsSentient(Human host, Human target) {
		for (War war: host.society.warsInvolved) {
			if (war.getOppositeSide(host.society).contains(target)) {
				return true;
			}
		}
		return false;
	}
	
}
