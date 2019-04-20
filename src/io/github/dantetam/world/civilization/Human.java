package io.github.dantetam.world.civilization;

import java.util.HashSet;
import java.util.Set;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.grid.LocalBuilding;

public class Human extends LivingEntity {

	public int hydration, maxHydration, nutrition, maxNutrition, rest, maxRest;
	
	public LocalBuilding home;
	
	public Set<Vector3i> allClaims;

	public SkillBook skillBook;
	
	public Human(String name) {
		super(name);
		allClaims = new HashSet<>();
		maxHydration = 100;
		maxNutrition = 100;
		maxRest = 100;
		hydration = 50;
		nutrition = 20;
		rest = 0;
		skillBook = new SkillBook();
	}

}
