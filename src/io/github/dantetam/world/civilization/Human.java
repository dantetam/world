package io.github.dantetam.world.civilization;

import java.util.HashSet;
import java.util.Set;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.grid.LocalBuilding;

public class Human extends LivingEntity {

	private static final double NUTRITION_CONSTANT = 10;
	private static final double REST_CONSTANT_TICK = 100 / (6 * 60);
	
	private static final double NUTRI_CONST_LOSS_TICK = 100 / (24 * 60);
	private static final double LIVE_CONST_LOSS_TICK = 100 / (18 * 60);
	
	public double hydration, maxHydration, nutrition, maxNutrition, rest, maxRest;
	
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
		nutrition = 30;
		rest = 80;
		skillBook = new SkillBook();
	}
	
	public void feed(double standardUnitNutrition) {
		nutrition = Math.min(nutrition + standardUnitNutrition*NUTRITION_CONSTANT, 
				maxNutrition);
	}
	
	public void rest(double standardRestUnit) {
		rest = Math.min(rest + standardRestUnit*REST_CONSTANT_TICK, maxRest);
	}
	
	public void spendNutrition() {
		nutrition = Math.max(nutrition - NUTRI_CONST_LOSS_TICK, 0);
	}
	
	public void spendEnergy() {
		rest = Math.max(rest - LIVE_CONST_LOSS_TICK, 0);
	}

}
