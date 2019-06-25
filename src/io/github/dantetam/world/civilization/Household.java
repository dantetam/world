package io.github.dantetam.world.civilization;

import java.util.Collection;
import java.util.List;

import io.github.dantetam.world.civhumanrelation.HumanHumanRel;
import io.github.dantetam.world.life.Human;

public class Household {

	public Society society;
	public Human headOfHousehold;
	public List<Human> householdMembers; //Includes the head of the household as well
	
	public Household(List<Human> householdMembers) {
		this.householdMembers = householdMembers;
	}
	
	public HumanHumanRel householdGetHumanRel(Human target) {
		if (this.headOfHousehold == null) {
			return null;
		}
		return this.headOfHousehold.brain.getHumanRel(target);
	}
	
	public HumanHumanRel householdGetHouseRel(Household house) {
		if (this.headOfHousehold != null && house.headOfHousehold != null) {
			return this.headOfHousehold.brain.getHumanRel(house.headOfHousehold);
		}
		return null;
	}
	
	public double getTotalWealth() {
		double sumWealth = 0;
		for (Human human: this.householdMembers) {
			sumWealth += human.getTotalWealth();
		}
		return sumWealth;
 	}
	
	public void removePeopleOutHouse(Collection<Human> humans) {
		householdMembers.removeAll(humans);
	}
	
}
