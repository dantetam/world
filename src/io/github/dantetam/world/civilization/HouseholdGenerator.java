package io.github.dantetam.world.civilization;

import java.util.ArrayList;
import java.util.List;

import io.github.dantetam.toolbox.StringUtil;
import io.github.dantetam.world.life.Human;

public class HouseholdGenerator {

	public static Household createHousehold(Society society) {
		Household household = new Household(StringUtil.genAlphaNumericStr(12) + "Test House", new ArrayList<>());
		List<Human> humans = new ArrayList<>();
		
		/*
		Human lord = new Human(society, "Lord " + familyName);
		humans.add(lord);
		
		Human lady = new Human(society, "Lady " + familyName);
		humans.add(lord);
		*/
		
		for (Human human: humans) {
			household.householdMembers.add(human);
		}
		
		return household;
	}
	
}
