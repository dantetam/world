package io.github.dantetam.world.civhumansocietyai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.toolbox.MathAndDistrUti;
import io.github.dantetam.vector.Vector3i;
import io.github.dantetam.world.civhumanai.Ethos;
import io.github.dantetam.world.civhumanai.EthosSet;
import io.github.dantetam.world.civhumanrelation.HumanHumanRel;
import io.github.dantetam.world.civhumanrelation.HumanHumanRel.HumanHumanRelType;
import io.github.dantetam.world.civhumanrelation.HumanSocietyRel;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.dataparse.SkillData;
import io.github.dantetam.world.grid.ClusterVector3i;
import io.github.dantetam.world.grid.GridRectInterval;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.grid.LocalGridLandClaim;
import io.github.dantetam.world.grid.SpaceFillingAlg;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.process.LocalJob;
import io.github.dantetam.world.process.LocalProcess;
import io.github.dantetam.world.process.LocalSocietyJob;
import io.github.dantetam.world.process.priority.SoldierPriority;

/**
 * 
 * @author Dante
 *
 */

public class SocietalHumansActionsCalc {
	
	//See EmergentSocietyCalc.java
	//Use societal/human ethos in calc. Add more unique actions with consequences.
	
	public static double calcPropensityToMarry(Human human, Human otherHuman, Date date) {
		HumanHumanRel oneWayRel = human.brain.getHumanRel(otherHuman);
		if (oneWayRel != null) {
			oneWayRel.reevaluateOpinion(date);
			double opinion = oneWayRel.opinion / 50;
			
			double attraction = oneWayRel.emotionGamut.getEmotion("Attraction") / 20;
			
			return PropensityUtil.nonlinearRelUtil(opinion + attraction);
		}
		return 0;
	}
	
	/**
	 * @param proposer
	 * @param target
	 * @return True if the marriage proposal went successfully for these people
	 */
	public static boolean proposeMarriage(Human proposer, Human target) {
		return true;
	}
	
	public static double calcPropensityToChat(Human human, Human otherHuman, Date date) {
		HumanHumanRel oneWayRel = human.brain.getHumanRel(otherHuman);
		if (oneWayRel != null) {
			oneWayRel.reevaluateOpinion(date);
			if (oneWayRel.opinion <= -30) return 0;
			
			//Use gregariousness/openness ethos to factor into this utility (one and two way)
			Ethos ethosA = human.brain.ethosSet.greatEthos.get("Extroverted");
			Ethos ethosB = otherHuman.brain.ethosSet.greatEthos.get("Extroverted");
			
			double util = PropensityUtil.nonlinearRelUtil((oneWayRel.opinion - (-30)) / 30);
			util += ethosA.getLogisticVal(-2, 6);
			util += ethosB.getLogisticVal(-1, 2);
			return util;
		}
		return 0;
	}
	
	//Intended to be calculated on all members of a society
	public static List<Human[]> possibleMarriagePairs(List<Human> humans, Date date) {
		List<Human[]> pairs = new ArrayList<>();
		for (int i = 0; i < humans.size(); i++) {
			double maxPros = 0;
			Human bestHumanToMarry = null;
			Human human = humans.get(i);
			for (int j = 0; j < humans.size(); j++) {
				Human otherHuman = humans.get(j);
				if (human.equals(otherHuman)) {
					continue;
				}
				double prosA = calcPropensityToMarry(human, otherHuman, date);
				double prosB = calcPropensityToMarry(otherHuman, human, date);
				if (prosA + prosB > 6 && prosA > 1.5 && prosB > 1.5) {
					if (prosA + prosB > maxPros || bestHumanToMarry == null) {
						maxPros = prosA + prosB;
						bestHumanToMarry = otherHuman;
					}
				}
			}
			if (bestHumanToMarry != null) {
				pairs.add(new Human[] {human, bestHumanToMarry});
			}
		}
		return pairs;
	}
	
	public static List<Human[]> possibleCordialPairs(List<Human> humans, Date date) {
		List<Human[]> pairs = new ArrayList<>();
		for (int i = 0; i < humans.size(); i++) {
			Human human = humans.get(i);
			for (int j = 0; j < humans.size(); j++) {
				Human otherHuman = humans.get(j);
				if (human.equals(otherHuman)) {
					continue;
				}
				HumanHumanRel rel = human.brain.getHumanRel(otherHuman);
				if (rel == null || rel.opinion < 0) {
					continue;
				}
				double prosA = calcPropensityToChat(human, otherHuman, date);
				double prosB = calcPropensityToChat(otherHuman, human, date);
				if (prosA + prosB > 3) {
					pairs.add(new Human[] {human, otherHuman});
				}
			}
		}
		return pairs;
	}
	
	public static List<Human[]> possibleBirthChildPairs(List<Human> humans, Date date) {
		List<Human[]> pairs = new ArrayList<>();
		for (int i = 0; i < humans.size(); i++) {
			double maxPros = 0;
			Human bestHumanToMarry = null;
			Human human = humans.get(i);
			for (int j = 0; j < humans.size(); j++) {
				Human otherHuman = humans.get(j);
				if (human.equals(otherHuman)) {
					continue;
				}
				HumanHumanRel rel = human.brain.getHumanRel(otherHuman);
				
				double prosA = calcPropensityToMarry(human, otherHuman, date);
				double prosB = calcPropensityToMarry(otherHuman, human, date);
				double prosTogether = rel.relationshipType == HumanHumanRelType.MARRIAGE ? 1.5 : 0;
				
				Household house = human.household;
				if (human.household.equals(otherHuman.household)) {
					if (house.size() > 3) {
						prosTogether -= 0.5 * (house.size() - 3);
					}
				}
				else {
					Household otherHouse = otherHuman.household;
					if (house.size() > 3) {
						prosTogether -= 0.666 * 0.5 * (house.size() - 3);
					}
					if (otherHouse.size() > 3) {
						prosTogether -= 0.666 * 0.5 * (otherHouse.size() - 3);
					}
				}
				
				if (prosA + prosB + prosTogether > 4 && prosA > 1 && prosB > 1) {
					if (prosA + prosB > maxPros || bestHumanToMarry == null) {
						maxPros = prosA + prosB;
						bestHumanToMarry = otherHuman;
					}
				}
			}
			if (bestHumanToMarry != null) {
				pairs.add(new Human[] {human, bestHumanToMarry});
			}
		}
		return pairs;
	}
	
	public static List<Human[]> getIdeoEthosDebatePairs(List<Human> humans, Date date) {
		List<Human[]> pairs = new ArrayList<>();
		for (int i = 0; i < humans.size(); i++) {
			Human human = humans.get(i);
			for (int j = 0; j < humans.size(); j++) {
				Human otherHuman = humans.get(j);
				if (human.equals(otherHuman)) {
					continue;
				}
				HumanHumanRel rel = human.brain.getHumanRel(otherHuman);
				if (rel == null || rel.opinion < 0) {
					continue;
				}
				double prosA = calcPropensityToChat(human, otherHuman, date);
				double prosB = calcPropensityToChat(otherHuman, human, date);
				
				double ideoDivide = EthosSet.getTotalEthosDifference(human.brain.ethosSet, otherHuman.brain.ethosSet);
				
				//Function that scales up with respect to high relations or lukewarm relations and significant ideological divide
				//People want to convert others to their ideology,
				//but also talk to others of their ideology.
				double utilGood = prosA + prosB;
				
				double possNegRel = Math.min(MathAndDistrUti.relu(-prosA - prosB), Math.max(prosA, prosB));
				double utilDivide = possNegRel * 0.3 + ideoDivide;
				
				if (utilGood > 3 || utilDivide > 3) {
					pairs.add(new Human[] {human, otherHuman});
				}
			}
		}
		return pairs;
	}
	
	/**
	 * TODO
	 * Figure out a system for people claiming land in a world
	 * @param grid
	 * @param society
	 * @param humans
	 * More powerful people get first choice? Followed by those who desparately want land and have the power
	 *	to obtain/maintain land
	 *
	 * Also combine with metrics of people's estimated land use e.g. wanting to create twenty shirts,
	 * which requires a hundred cotton plants, for approximately 150 tiles for this new farm/tailor complex.
	 * See AnnotatedRoom data.
	 */
	public static Map<Human, GridRectInterval> possibleNewLandClaims(LocalGrid grid, Society society,
			List<Human> humans) {
		Map<Human, GridRectInterval> humanClaimUtil = new HashMap<>();
		
		Set<Vector3i> claimedFirstVec = new HashSet<>();
		
		List<Human> humansSortedByPrestige = new ArrayList<>(humans);
		humansSortedByPrestige.sort(new Comparator<Human>() { //Descending sort
			@Override
			public int compare(Human o1, Human o2) {
				return (int) (o2.getTotalPowerPrestige() - o1.getTotalPowerPrestige());
			}
		});
		
		for (Human human: humansSortedByPrestige) {
			Map<GridRectInterval, Double> spaceScoring = new HashMap<>();
			
			int claimedLandArea = 0;
			for (LocalGridLandClaim claim: human.allClaims) {
				claimedLandArea += claim.boundary.get2dSize();
			}
			int optimalLandArea = (int) human.getTotalPowerPrestige();
			int landValueTile = 10;
			int landNeed = Math.max(400, (optimalLandArea - claimedLandArea) / landValueTile);
			int dimension = (int) Math.sqrt(landNeed);
			
			if (landNeed > 0) {
				for (ClusterVector3i clusterObj: grid.clustersList3d) {
					Vector3i firstVec = clusterObj.clusterData.iterator().next();
					GridRectInterval space = SpaceFillingAlg.findAvailSpaceClose(
							grid, firstVec, dimension, dimension, false, society, null, false, null, null);
					
					if (space != null) {
						int size = (int) Math.abs(space.get2dSize() - dimension * dimension);
						int dist = human.location.coords.manhattanDist(firstVec);
						double score = size + dist - dimension;
						spaceScoring.put(space, score);
					}
				}
			}
			
			//Do something in future with human ranking of different land parcels
			spaceScoring = MapUtil.getSortedMapByValueDesc(spaceScoring);
			for (Object object: spaceScoring.keySet().toArray()) {
				GridRectInterval cluster = (GridRectInterval) object;
				Vector3i firstVec = cluster.avgVec();
				if (!claimedFirstVec.contains(firstVec)) {
					claimedFirstVec.add(firstVec);
					humanClaimUtil.put(human, cluster);
				}
			}
		}
		
		return humanClaimUtil;
	}
	
	/**
	 * 
	 * @param host    The society that is considering raiding/attacking other societies for war or plunder
	 * @param humans  Humans in question from the host society
	 * @return The possible raid party (groups of humans) for use in free actions
	 */
	public static Map<Human, Double> possibleRaidingPartyUtil(Society host, List<Human> humans) {
		Map<Human, Double> fightMilitaryUtil = new HashMap<>();
		for (Human human: humans) {
			double util = 0;
			
			double baseEquipScore = human.body.getNumMainBodyParts() * 2;
			double weaponReq = SoldierPriority.weaponAmtRequired(human);
			double armorReq = SoldierPriority.armorAmtRequired(human);
			util += (baseEquipScore - weaponReq - armorReq) / baseEquipScore;
			
			double ethosScore = 0;
			Ethos warEthos = human.brain.ethosSet.getEthosMapping().get("Belligerent");
			ethosScore += warEthos.getNormLogisticVal();
			util += ethosScore;
			
			fightMilitaryUtil.put(human, util);
		}
		return fightMilitaryUtil;
	}
	
	/**
	 * Determine the wealth and current job processes of every human in this society,
	 * in line with Society.java::prioritizeProcesses (of this pipeline),
	 * and determine the propensity of every human to hire others as soldiers, workers, etc.
	 * @param humans
	 * @param date
	 * @return A mapping of humans that wish to employ others, with the associated potential utility
	 */
	/*
	public static Map<Human, Double> possibleEmployerUtil(Human employee, Human boss, LocalJob job) {
		
	}
	*/
	
	//TODO
	public static double possibleEmployeeUtil(Human employee, Human boss, LocalJob job, Date date) {
		double emplBossRelUtil = calcPropensityToMarry(employee, boss, date);
		
		double leadLevel = boss.skillBook.getSkillLevel("Leadership");
		double leadershipCap = SkillData.leadershipCapPeople((int) leadLevel);
		double distToCap = leadershipCap - boss.workers.size();
		double numUtil = Math.min(distToCap / 10, 5.0);
		
		Ethos obedEthos = employee.brain.ethosSet.getEthosMapping().get("Obedient");
		double obedUtil = obedEthos.getLogisticVal(-2.0, 2.0);
		
		emplBossRelUtil += numUtil + obedUtil; 
		
		return emplBossRelUtil;
	}

	//TODO: Factor in opinions of neighbors and other people who live in society?
	public static double employeeSocietyUtil(Human employee, Society societyJob, LocalSocietyJob value,
			Date date) {
		double socUtil = 0;
		HumanSocietyRel socRel = employee.brain.getSocRel(societyJob); 
		socUtil += socRel.opinion;
		
		double avgUtil = 0;
		List<Human> leaders = societyJob.leadershipManager.currentLeaders;
		for (Human boss: leaders) {
			double emplBossRelUtil = calcPropensityToMarry(employee, boss, date);
			
			double leadLevel = boss.skillBook.getSkillLevel("Leadership");
			double leadershipCap = SkillData.leadershipCapPeople((int) leadLevel);
			double distToCap = leadershipCap - boss.workers.size();
			double numUtil = Math.min(distToCap / 10, 5.0);
			
			Ethos obedEthos = employee.brain.ethosSet.getEthosMapping().get("Obedient");
			double obedUtil = obedEthos.getLogisticVal(-2.0, 2.0);
			
			avgUtil += emplBossRelUtil + numUtil + obedUtil; 
		}
		avgUtil /= leaders.size();
		
		return (socUtil + avgUtil) / 2;
	}
	
}
