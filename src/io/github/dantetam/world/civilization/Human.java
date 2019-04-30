package io.github.dantetam.world.civilization;

import java.util.HashSet;
import java.util.Set;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.dataparse.ClothingData;
import io.github.dantetam.world.dataparse.AnatomyData.Body;
import io.github.dantetam.world.grid.LocalBuilding;

public class Human extends LivingEntity {
	
	public LocalBuilding home;
	
	public Set<Vector3i> allClaims;

	public SkillBook skillBook;
	
	public String familyName;
	
	public Human(String name) {
		super(name);
		allClaims = new HashSet<>();
		maxNutrition = 100;
		maxRest = 100;
		nutrition = 30;
		rest = 80;
		skillBook = new SkillBook();
		body = new Body();
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
