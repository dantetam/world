package io.github.dantetam.world.civhumansocietyai;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Map.Entry;
import java.util.HashMap;

import io.github.dantetam.toolbox.CollectionUtil;
import io.github.dantetam.toolbox.Pair;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civhumanai.Ethos;
import io.github.dantetam.world.civhumanai.EthosSet;
import io.github.dantetam.world.civhumanrelation.HumanHumanRel;
import io.github.dantetam.world.civhumanrelation.HumanHumanRel.HumanHumanRelType;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.civilization.LocalExperience;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.WorldGrid;
import io.github.dantetam.world.life.Human;

public class FreeActionsHumans {

	public static Map<String, FreeAction> freeActionsListHuman = new HashMap<String, FreeAction>() {{
		put("formNewHouseMarriage", new FreeAction("formNewHouseMarriage", null, 30));
		put("tryToHaveChild", new FreeAction("tryToHaveChild", null, 15));
		put("claimNewLand", new FreeAction("claimNewLand", null, 15));
		
		put("chat", new FreeAction("chat", null, 1));
		put("ideologicalEthosDebate", new FreeAction("ideologicalEthosDebate", null, 5));
	}};
	
	//Implement more human to human interactions, and for all interactions, take into account
	//the situation, the relationships, and relevant personality traits.
	
	public static void considerAllFreeActionsHumans(WorldGrid world, LocalGrid grid,
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
						Household houseC = new Household(allHumansHouseC);
						Society host = houseA.society;
						host.addHousehold(houseC);
					}
				}
			}
			else if (name.equals("tryToHaveChild")) {
				//TODO;
			}
			else if (name.equals("claimNewLand")) {
				Map<Human, Set<Vector3i>> humanClaimUtil = SocietalHumansActionsCalc
						.possibleNewLandClaims(grid, humans);
				for (Entry<Human, Set<Vector3i>> claimEntry: humanClaimUtil.entrySet()) {
					Human human = claimEntry.getKey();
					Set<Vector3i> cluster = claimEntry.getValue();
					
					//TODO;
				}
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
					double conviction = Math.log(debatingEthos.first.severity);
					double civility = 0.5 * (relA.opinion + relB.opinion) / 30;
					
					LocalExperience exp = new LocalExperience("EthosDebate");
					exp.modifiers = new ArrayList<String>() {{add(debatingEthos.first.name);}};
					exp.valueModifiers = new HashMap<String, Double>() {{put("conviction", conviction);}};
					exp.valueModifiers = new HashMap<String, Double>() {{put("civility", civility);}};
					exp.beingRoles.put(humanA, CollectionUtil.newSet("chatInitiate"));
					exp.beingRoles.put(humanB, CollectionUtil.newSet("chat"));
				}
				
			}
		}
	}
	
}
