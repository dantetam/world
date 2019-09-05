package io.github.dantetam.world.life;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.toolbox.CollectionUtil;
import io.github.dantetam.world.civhumanai.NeedsGamut;
import io.github.dantetam.world.civhumanrelation.HumanHumanRel;
import io.github.dantetam.world.civhumanrelation.HumanHumanRel.HumanHumanRelType;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.civilization.LocalExperience;
import io.github.dantetam.world.civilization.SkillBook;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.ItemData;
import io.github.dantetam.world.grid.LocalBuilding;
import io.github.dantetam.world.grid.LocalGridLandClaim;
import io.github.dantetam.world.grid.LocalGridTimeExecution;
import io.github.dantetam.world.items.Properties;
import io.github.dantetam.world.process.LocalJob;
import io.github.dantetam.world.process.LocalProcess;

public class Human extends LivingEntity {
	
	public Society society;
	public LocalBuilding home;
	
	public Set<LocalGridLandClaim> allClaims;
	//public Map<LocalProcess, List<LocalGridLandClaim>> allClaimsByPurpose;
	
	public SkillBook skillBook;
	public String familyName;
	
	public HumanBrain brain;
	public DNAHuman dna; 
	
	//Used for convenient storage; all humans have relationships stored in HumanBrain::indexedRelationships
	public Human lord;
	public List<Human> servants;
	public Map<Human, Set<LocalJob>> workers;
	
	public Household household;
	
	public List<LivingEntity> ownedAnimals;
	
	public Properties properties; //Use traits like in DF to represent physical properties of a person, like sentience, or civilization,
			//or aversion to water.
	
	public NeedsGamut shortTermNeeds, mediumTermNeeds, lifetimeNeeds;
	//TODO;
	//Implement short-term (immediate), medium-term (job? current goals), 
	//and long-term (fulfillment and life-long) Maslow needs
	
	public Human(Society society, String name, String speciesName) {
		super(name);
		this.society = society;
		allClaims = new HashSet<>();
		maxNutrition = 100;
		maxRest = 100;
		nutrition = 30;
		rest = 80;
		skillBook = new SkillBook();
		body = new Body(speciesName);
		brain = new HumanBrain(this);
		EthosSetInitialize.initHumanBrain(brain.ethosSet);
		dna = new DNAHuman(speciesName);
		servants = new ArrayList<>();
		workers = new HashMap<>();
		ownedAnimals = new ArrayList<>();
		properties = new Properties();
		shortTermNeeds = new NeedsGamut();
		mediumTermNeeds = new NeedsGamut();
		lifetimeNeeds = new NeedsGamut();
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
	
	public double raceSimilarityScore(Human human) {
		return this.dna.compareGenesDist(human.dna, "race");
	}
	
	@Override
	public double getTotalPowerPrestige() {
		double basePrestige = super.getTotalPowerPrestige();
		if (household != null) { //Count the prestige of other house members
			double housePrestigeExtra = 0;
			for (Human houseMember: household.householdMembers) {
				if (houseMember.equals(this)) continue;
				housePrestigeExtra += houseMember.getTotalPowerPrestige();
			}
			if (!this.equals(household.headOfHousehold)) {
				housePrestigeExtra *= 0.3;
			}
			basePrestige += housePrestigeExtra;
		}
		
		for (Human worker: workers.keySet()) {
			if (!worker.equals(this)) {
				basePrestige += worker.getTotalPowerPrestige() * 0.3;
			}
		}
		for (Human servant: servants) {
			basePrestige += servant.getTotalPowerPrestige();
		}
		
		return basePrestige;
	}
	
	@Override
	public double getTotalWealth() {
		double baseWealth = super.getTotalWealth();
		
		for (Human servant: servants) {
			baseWealth += servant.getTotalWealth();
		}
		
		int landClaimId = ItemData.getIdFromName("Land Claim");
		double landVal = 10;
		if (society.calcUtility != null && society.calcUtility.containsKey(landClaimId)) {
			landVal = society.calcUtility.get(landClaimId) / 10.0;
		}
		
		for (LocalGridLandClaim claim: this.allClaims) {
			baseWealth += claim.boundary.get2dSize() * landVal;
		}
		
		return baseWealth;
	}

	public void addServant(Human candidate) {
		//TODO: Check for circular references for lord-servant relations 
	}
	
	public void getMarried(Human fiance) {
		LocalExperience marriageExp = new LocalExperience("Marriage");
		marriageExp.beingRoles.put(this, CollectionUtil.newSet("suitor"));
		marriageExp.beingRoles.put(fiance, CollectionUtil.newSet("fiance"));
		
		HumanHumanRel rel = this.brain.getHumanRel(fiance);
		rel.relationshipType = HumanHumanRelType.MARRIAGE;
		rel.sharedExperiences.add(marriageExp);
		
		rel = fiance.brain.getHumanRel(this);
		rel.relationshipType = HumanHumanRelType.MARRIAGE;
		rel.sharedExperiences.add(marriageExp);
		
		brain.experiences.add(marriageExp);
	}
	
}
