package io.github.dantetam.world.life;

import java.util.HashSet;
import java.util.Set;

import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civilization.SkillBook;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.AnatomyData.Body;
import io.github.dantetam.world.grid.LocalBuilding;

public class Human extends LivingEntity {
	
	public Society society;
	public LocalBuilding home;
	public Set<Vector3i> allClaims;
	public SkillBook skillBook;
	public String familyName;
	
	public HumanBrain brain;
	
	public Human(Society society, String name) {
		super(name);
		this.society = society;
		allClaims = new HashSet<>();
		maxNutrition = 100;
		maxRest = 100;
		nutrition = 30;
		rest = 80;
		skillBook = new SkillBook();
		body = new Body("Human");
		brain = new HumanBrain(this);
		HumanBrainInitialize.initHumanBrain(brain);
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
