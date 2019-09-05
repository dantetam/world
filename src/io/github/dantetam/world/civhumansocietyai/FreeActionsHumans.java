package io.github.dantetam.world.civhumansocietyai;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.distribution.GeometricDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Map.Entry;
import java.util.HashMap;

import io.github.dantetam.toolbox.CollectionUtil;
import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.Pair;
import io.github.dantetam.world.civhumanai.Ethos;
import io.github.dantetam.world.civhumanai.EthosSet;
import io.github.dantetam.world.civhumanrelation.HumanHumanRel;
import io.github.dantetam.world.civhumanrelation.HumanHumanRel.HumanHumanRelType;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.civilization.LocalExperience;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.civilization.gridstructure.PurposeAnnotatedBuild;
import io.github.dantetam.world.dataparse.ProcessData;
import io.github.dantetam.world.dataparse.SkillData;
import io.github.dantetam.world.grid.GridRectInterval;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalGridLandClaim;
import io.github.dantetam.world.grid.WorldGrid;
import io.github.dantetam.world.life.DNAHuman;
import io.github.dantetam.world.life.Human;

public class FreeActionsHumans {

	public static Map<String, FreeAction> freeActionsListHuman = new HashMap<String, FreeAction>() {{
		put("formNewHouseMarriage", new FreeAction("formNewHouseMarriage", null, 30));
		put("tryToHaveChild", new FreeAction("tryToHaveChild", null, 15));
		put("claimNewLand", new FreeAction("claimNewLand", null, 1));
		
		put("buildBasicHome", new FreeAction("buildBasicHome", null, 2));
		put("improveComplex", new FreeAction("improveComplex", null, 2));
		
		put("chat", new FreeAction("chat", null, 1));
		put("ideologicalEthosDebate", new FreeAction("ideologicalEthosDebate", null, 5));
	}};
	
	//Implement more human to human interactions, and for all interactions, take into account
	//the situation, the relationships, and relevant personality traits.
	
	public static void considerAllFreeActionsHumans(WorldGrid world, LocalGrid grid, Society society,
			List<Human> humans, Date date) {
		for (Entry<String, FreeAction> entry: freeActionsListHuman.entrySet()) {
			if (!entry.getValue().fireChanceExecute()) continue;
			String name = entry.getKey();
			if (name.equals("formNewHouseMarriage")) {
				List<Human[]> marriagePairs = SocietalHumansActionsCalc.possibleMarriagePairs(humans, date);
				if (marriagePairs.size() == 0) continue;
				int randIndex = (int) (Math.random() * marriagePairs.size());
				Human[] pair = marriagePairs.get(randIndex);
				Human proposer = pair[0], target = pair[1];
				//Try a marriage attempt between these two people, such that these two people
				//form a new household.
				if (SocietalHumansActionsCalc.proposeMarriage(proposer, target)) {
					proposer.getMarried(target);
					
					Household houseA = proposer.household;
					Household houseB = target.household;
					
					List<Human> allHumansHouseC = new ArrayList<>();
					if (!proposer.equals(houseA.headOfHousehold)) {
						List<List<Human>> newHousesDataA = EmergentHouseholdCalc.divideHouseholdsBySeparating(
								houseA, new ArrayList<Human>() {{add(proposer); add(target);}}, date);
						List<Human> movingOutOfA = newHousesDataA.get(1);
						houseA.removePeopleOutHouse(movingOutOfA);
						allHumansHouseC.addAll(movingOutOfA);
					}
					if (!houseB.equals(houseA) && !target.equals(houseB.headOfHousehold)) {
						List<List<Human>> newHousesDataB = EmergentHouseholdCalc.divideHouseholdsBySeparating(
								houseB, new ArrayList<Human>() {{add(proposer); add(target);}}, date);
						List<Human> movingOutOfB = newHousesDataB.get(1);
						houseB.removePeopleOutHouse(movingOutOfB);
						allHumansHouseC.addAll(movingOutOfB);
					}
					if (allHumansHouseC.size() > 0) {
						Human firstPerson = allHumansHouseC.get(0);
						Household houseC = new Household(firstPerson.familyName + " Household", allHumansHouseC);
						Society host = houseA.society;
						host.addHousehold(houseC);
					}
				}
			}
			else if (name.equals("tryToHaveChild")) {
				List<Human[]> makeChildPairs = SocietalHumansActionsCalc.possibleBirthChildPairs(humans, date);
				if (makeChildPairs.size() == 0) continue;
				int randIndex = (int) (Math.random() * makeChildPairs.size());
				Human[] pair = makeChildPairs.get(randIndex);
				Human proposer = pair[0], target = pair[1];
				
				DNAHuman newHumanDna = proposer.dna.recombineDNA(target.dna);
				Human child = new Human(proposer.society, "Child " + proposer.household.size() + 
						proposer.household.name, proposer.dna.speciesName);
				proposer.household.addPeopleHouse(child);
				child.dna = newHumanDna;
				
				proposer.brain.addHumanRel(child, HumanHumanRelType.FAMILY);
				
				LocalExperience childExp = new LocalExperience("Child Birth");
				Human father = proposer.dna.getDnaMapping("sex").equals("XY") ? proposer : target;
				Human mother = proposer.equals(father) ? target : proposer;
				childExp.beingRoles.put(father, CollectionUtil.newSet("Father"));
				childExp.beingRoles.put(mother, CollectionUtil.newSet("Mother"));
				
				HumanHumanRel rel = proposer.brain.getHumanRel(target);
				rel.sharedExperiences.add(childExp);
			}
			else if (name.equals("claimNewLand")) {
				Map<Human, GridRectInterval> humanClaimUtil = SocietalHumansActionsCalc
						.possibleNewLandClaims(grid, society, humans);
				for (Entry<Human, GridRectInterval> claimEntry: humanClaimUtil.entrySet()) {
					Human human = claimEntry.getKey();
					GridRectInterval interval = claimEntry.getValue();
					
					//for (GridRectInterval interval: cluster) {
						List<Human> claimants = grid.findClaimantToTiles(interval);
						if (claimants != null && claimants.size() > 0) {
							continue;
						}
						grid.claimTiles(human, interval.getStart(), interval.getEnd(), null);
					//}
				}
			}
			else if (name.equals("buildBasicHome")) {
				int randIndex = (int) (Math.random() * humans.size());
				Human randomHuman = humans.get(randIndex);
				randomHuman.queuedProcesses.add(ProcessData.getProcessByName("Build Basic Home"));
			}
			else if (name.equals("improveComplex")) {
				Map<Human, Double> humanImprScore = new HashMap<>();
				for (Human human: humans) {
					int num = human.designatedBuildsByPurpose.size();
					if (num > 0) { 
						double scoring = 0;
						for (List<PurposeAnnotatedBuild> annoBuilds: human.designatedBuildsByPurpose.values()) {
							for (PurposeAnnotatedBuild annoBuild: annoBuilds) {
								scoring += 2 + annoBuild.totalArea();
							}
						}
						humanImprScore.put(human, scoring);
					}
				}
				humanImprScore = MapUtil.getSortedMapByValueDesc(humanImprScore);
				Human randomHuman = MapUtil.randChoiceFromWeightMap(humanImprScore);
				randomHuman.queuedProcesses.add(ProcessData.getProcessByName("Improve Complex"));
			}
			else if (name.equals("chat")) { //Temporarily represent chatting as an instaneous free action
				List<Human[]> chatPairs = SocietalHumansActionsCalc.possibleCordialPairs(humans, date);
				for (Human[] chatPair: chatPairs) {
					Human humanA = chatPair[0];
					Human humanB = chatPair[1];
					
					LocalExperience exp = new LocalExperience("Chat");
					exp.beingRoles.put(humanA, CollectionUtil.newSet("chatInitiate"));
					exp.beingRoles.put(humanB, CollectionUtil.newSet("chat"));
					
					HumanHumanRel rel = humanA.brain.getHumanRel(humanB);
					rel.sharedExperiences.add(exp);
					
					rel = humanB.brain.getHumanRel(humanA);
					rel.sharedExperiences.add(exp);
				}
			}
			else if (name.equals("ideologicalEthosDebate")) {
				//People in good relationships talk about both common and differing beliefs
				//(reinforcement and rational debate, respectively).
				
				//People in bad relationships talk about differing beliefs only (non-civil debate)
				
				List<Human[]> chatPairs = SocietalHumansActionsCalc.getIdeoEthosDebatePairs(humans, date);
				for (Human[] chatPair: chatPairs) {
					Human humanA = chatPair[0];
					Human humanB = chatPair[1];
					
					HumanHumanRel relA = humanA.brain.getHumanRel(humanB);
					HumanHumanRel relB = humanB.brain.getHumanRel(humanA);
					double effRel = Math.min(relA.opinion, relB.opinion);
					
					double differingBoundLeft = Math.pow(Math.abs(effRel - 30) / 30, 1.25);
					double similarBoundRight = (effRel - 30) / 30 * 5;
					
					double mean = (differingBoundLeft + similarBoundRight) / 2.0;
					double sd = (similarBoundRight - mean) / 2.3;
					NormalDistribution distr = new NormalDistribution(mean, sd);
					double getDiffRating = distr.sample();
					
					Pair<Ethos> debatingEthos = EthosSet.getClosestEthosWithDiffVal(
							humanA.brain.ethosSet, humanB.brain.ethosSet, getDiffRating);
					double convictionA = Math.log(debatingEthos.first.severity);
					double convictionB = Math.log(debatingEthos.second.severity);
					double civility = 0.5 * (relA.opinion + relB.opinion) / 30;
					
					double rheSkillA = humanA.skillBook.getSkillLevel("Rhetoric");
					double rheSkillB = humanB.skillBook.getSkillLevel("Rhetoric");
					double argumentScoreA = SkillData.rhetoricArgumentScore((int) (rheSkillA + rheSkillB / 4)) * convictionA;
					double argumentScoreB = SkillData.rhetoricArgumentScore((int) (rheSkillB + rheSkillA / 4)) * convictionB;
					
					double directionFirst = Math.signum(debatingEthos.second.severity - debatingEthos.first.severity);
					double directionSecond = Math.signum(debatingEthos.first.severity - debatingEthos.second.severity);
					debatingEthos.first.severity += directionFirst * argumentScoreB * civility;
					debatingEthos.second.severity += directionSecond * argumentScoreA * civility;
					
					LocalExperience exp = new LocalExperience("EthosDebate");
					exp.modifiers = new ArrayList<String>() {{add(debatingEthos.first.name);}};
					exp.valueModifiers = new HashMap<String, Double>() {{put("conviction", (convictionA + convictionB) / 2);}};
					exp.valueModifiers = new HashMap<String, Double>() {{put("civility", civility);}};
					exp.beingRoles.put(humanA, CollectionUtil.newSet("chatInitiate"));
					exp.beingRoles.put(humanB, CollectionUtil.newSet("chat"));
				}
				
			}
		}
	}
	
}
