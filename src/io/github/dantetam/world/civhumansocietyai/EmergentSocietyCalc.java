package io.github.dantetam.world.civhumansocietyai;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.world.civhumanrelation.HumanHumanRel;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.life.Human;

/**
 * Provide utility methods for operating on society/household data,
 * which are used to provide decision data given to humans,
 * who can form new societies with other people.
 * 
 * People form societies with these common themes:
 * multiple families with similar ethos, race/family/marital connections, and proximity come together;
 * a family chooses to rule over others through might and strength;
 * a family uses its wealth to build a new society (relative). 
 * 
 * For technical reasons, the process of forming new societies, a group "free action"
 * (one that can be done at any time by consenting individuals),
 * is stored as a process (possibly at the society level?).
 * 
 * See FreeActionsSociety.java
 * 
 * @author Dante
 *
 */

public class EmergentSocietyCalc {

	/*
	public static List<Human> calcMaxSubgroupNoLeader(List<Human> humans, Date date, 
			String method, double cutoff) {
		List<Human> bestGroup = null;
		Human host = null;
		for (Human human: humans) {
			List<Human> candidateGroup = largestSubgroupProsGenHouse(human, humans, date, method, cutoff);
			if (candidateGroup != null) {
				if (bestGroup == null || candidateGroup.size() > bestGroup.size()) {
					bestGroup = candidateGroup;
					host = human;
				}
			}
		}
		if (bestGroup != null) {
			if (bestGroup.contains(host)) {
				
			}
		}
		return bestGroup;
	}
	*/
	
	public static List<Household> calcHouseholdGen(List<Household> humans, Date date, 
			String method, double cutoff) {
		List<Household> bestGroup = null;
		Household host = null;
		for (Household human: humans) {
			List<Household> candidateGroup = largestSubgroupProsGenSociety(
					human, humans, date, method, cutoff);
			if (candidateGroup != null) {
				if (bestGroup == null || candidateGroup.size() > bestGroup.size()) {
					bestGroup = candidateGroup;
					host = human;
				}
			}
		}
		if (bestGroup != null) {
			if (bestGroup.contains(host)) {
				bestGroup.remove(host);
				bestGroup.add(0, host);
			}
		}
		return bestGroup;
	}
	
	/**
	 * Form a maximum length group of people by adding people from the candidates one at a time 
	 * @param host    The person creating the society and its future leader
	 * @param humans  The potential candidates
	 * @param date    The date for which to update relationships on (based on fading memories)
	 * @param method  The choice of emergent society method (harmony, might, wealth)
	 * @param cutoff  The cutoff utility for which to stop expanding a potential group
	 * @return The largest subset of the group humans, such that these humans are OK forming a society
	 * 		   given the method and utility cutoff. 
	 */
	private static List<Human> largestSubgroupProsGenHouse(Human host, List<Human> humans, Date date, 
			String method, double cutoff) {
		if (humans.size() == 0) {
			//throw new IllegalArgumentException("Cannot find emergent societal groups from a group of zero humans");
			return null;
		}
		List<Set<Integer>> candidateGroups = new ArrayList<Set<Integer>>();  
		for (int i = 0; i < humans.size(); i++) {
			Set<Integer> startGroup = new HashSet<Integer>();
			startGroup.add(i);
			candidateGroups.add(startGroup);
		}
		Set<Integer> bestGroup = null;
		while (candidateGroups.size() > 0) {
			Set<Integer> group = candidateGroups.get(0);
			for (int i = 0; i < humans.size(); i++) {
				if (group.contains(i)) continue;
				List<Human> candidateHumans = new ArrayList<>();
				for (Integer groupMem: group) {
					candidateHumans.add(humans.get(groupMem));
				}
				candidateHumans.add(humans.get(i));
				double util;
				if (method.equals("harmony")) {
					util = calcProsHarmonyGenHouse(host, humans, date);
				}
				else if (method.equals("wealth")) {
					util = calcProsWealthGenHouse(host, humans, date);
				}
				else {
					util = calcProsTakeoverGenHouse(host, humans, date);
				}
				if (util > cutoff) {
					group.add(i);
					if (candidateHumans.size() > bestGroup.size() || bestGroup == null) {
						bestGroup = group;
					}
					candidateGroups.add(group);
				}
				else {
					continue;
				}
			}
		}
		List<Human> candidateHumans = new ArrayList<>();
		for (Integer groupMem: bestGroup) {
			candidateHumans.add(humans.get(groupMem));
		}
		return candidateHumans;
	}
	
	private static double calcProsHarmonyGenHouse(Human host, List<Human> humans, Date date) {
		double propensityUtil = 0;
		double numRel = humans.size() * (humans.size() - 1) / 2;
		for (Human human: humans) {
			for (Human otherHuman: humans) {
				if (human.equals(otherHuman)) {
					HumanHumanRel oneWayRel = human.brain.getHumanRel(otherHuman);
					if (oneWayRel != null) {
						oneWayRel.reevaluateOpinion(date);
						double opinion = oneWayRel.opinion / 50;
						propensityUtil += PropensityUtil.nonlinearRelUtil(opinion) / numRel;
					}
				}
			}
		}
		return propensityUtil;
	}
	
	private static double calcProsTakeoverGenHouse(Human host, List<Human> humans, Date date) {
		double propensityUtil = 0;
		double numRel = humans.size() - 1;

		double hostWealth = host.getTotalWealth();
		for (Human human: humans) {
			if (host.equals(human)) continue;
			double candidateWealth = human.getTotalWealth();
			HumanHumanRel oneWayRel = human.brain.getHumanRel(host);
			oneWayRel.reevaluateOpinion(date);
			double relUtil = PropensityUtil.nonlinearRelUtil(Math.signum(50 - oneWayRel.opinion) * Math.abs(oneWayRel.opinion - 50) / 50);
			if (hostWealth > human.getTotalWealth() * 0.8) {
				propensityUtil += relUtil * Math.max(3.5, hostWealth / candidateWealth);
			}
		}
		
		return propensityUtil / numRel;
	}
	
	private static double calcProsWealthGenHouse(Human host, List<Human> humans, Date date) {
		double propensityUtil = 0;
		double numRel = humans.size() - 1;
		
		//Find percentile of wealth of the host, adjusted to weighting with opinions and other external factors (not wealth)
		double hostWealth = host.getTotalWealth();
		for (Human human: humans) {
			if (host.equals(human)) continue;
			double candidateWealth = human.getTotalWealth();
			HumanHumanRel oneWayRel = human.brain.getHumanRel(host);
			oneWayRel.reevaluateOpinion(date);
			double relUtil = PropensityUtil.nonlinearRelUtil(Math.signum(oneWayRel.opinion - 50) * Math.abs(oneWayRel.opinion - 50) / 100);
			if (hostWealth > candidateWealth) {
				propensityUtil += relUtil * Math.max(2.5, hostWealth / candidateWealth);
			}
			else if (hostWealth > human.getTotalWealth() * 0.5) {
				propensityUtil += relUtil * 0.25;
			}
		}
		
		return propensityUtil / numRel;
	}
	
	private static List<Household> largestSubgroupProsGenSociety(Household host, List<Household> houses, Date date, 
			String method, double cutoff) {
		if (houses.size() == 0) {
			//throw new IllegalArgumentException("Cannot find emergent societal groups from a group of zero humans");
			return null;
		}
		List<Set<Integer>> candidateGroups = new ArrayList<Set<Integer>>();  
		for (int i = 0; i < houses.size(); i++) {
			Set<Integer> startGroup = new HashSet<Integer>();
			startGroup.add(i);
			candidateGroups.add(startGroup);
		}
		Set<Integer> bestGroup = null;
		while (candidateGroups.size() > 0) {
			Set<Integer> group = candidateGroups.get(0);
			for (int i = 0; i < houses.size(); i++) {
				if (group.contains(i)) continue;
				List<Household> candidateHumans = new ArrayList<>();
				for (Integer groupMem: group) {
					candidateHumans.add(houses.get(groupMem));
				}
				candidateHumans.add(houses.get(i));
				double util;
				if (method.equals("harmony")) {
					util = calcPropensityHarmonySociety(host, houses, date);
				}
				else if (method.equals("wealth")) {
					util = calcProsWealthPowerSociety(host, houses, date);
				}
				else {
					util = calcPropensityTakeoverSociety(host, houses, date);
				}
				if (util > cutoff) {
					group.add(i);
					if (candidateHumans.size() > bestGroup.size() || bestGroup == null) {
						bestGroup = group;
					}
					candidateGroups.add(group);
				}
				else {
					continue;
				}
			}
		}
		List<Household> candidateHumans = new ArrayList<>();
		for (Integer groupMem: bestGroup) {
			candidateHumans.add(houses.get(groupMem));
		}
		return candidateHumans;
	}
	
	private static double calcPropensityHarmonySociety(Household host, List<Household> houses, Date date) {
		double propensityUtil = 0;
		double numRel = houses.size() * (houses.size() - 1) / 2;
		for (Household human: houses) {
			for (Household otherHuman: houses) {
				if (human.equals(otherHuman)) {
					HumanHumanRel oneWayRel = human.householdGetHouseRel(otherHuman);
					oneWayRel.reevaluateOpinion(date);
					double opinion = oneWayRel.opinion / 50;
					if (oneWayRel != null) {
						propensityUtil += PropensityUtil.nonlinearRelUtil(opinion) / numRel;
					}
				}
			}
		}
		return propensityUtil;
	}
	
	private static double calcPropensityTakeoverSociety(Household host, List<Household> houses, Date date) {
		double propensityUtil = 0;
		double numRel = houses.size() - 1;

		double hostWealth = host.getTotalWealth();
		for (Household human: houses) {
			if (host.equals(human)) continue;
			double candidateWealth = human.getTotalWealth();
			HumanHumanRel oneWayRel = human.householdGetHouseRel(host);
			oneWayRel.reevaluateOpinion(date);
			double relUtil = PropensityUtil.nonlinearRelUtil(Math.signum(50 - oneWayRel.opinion) * Math.abs(oneWayRel.opinion - 50) / 50);
			if (hostWealth > human.getTotalWealth() * 0.8) {
				propensityUtil += relUtil * Math.max(3.5, hostWealth / candidateWealth);
			}
		}
		
		return propensityUtil / numRel;
	}
	
	private static double calcProsWealthPowerSociety(Household host, List<Household> houses, Date date) {
		double propensityUtil = 0;
		double numRel = houses.size() - 1;
		
		//Find percentile of wealth of the host, adjusted to weighting with opinions and other external factors (not wealth)
		double hostWealth = host.getTotalWealth();
		for (Household human: houses) {
			if (host.equals(human)) continue;
			double candidateWealth = human.getTotalWealth();
			HumanHumanRel oneWayRel = human.householdGetHouseRel(host);
			oneWayRel.reevaluateOpinion(date);
			double relUtil = PropensityUtil.nonlinearRelUtil(Math.signum(oneWayRel.opinion - 50) * Math.abs(oneWayRel.opinion - 50) / 100);
			if (hostWealth > candidateWealth) {
				propensityUtil += relUtil * Math.max(2.5, hostWealth / candidateWealth);
			}
			else if (hostWealth > human.getTotalWealth() * 0.5) {
				propensityUtil += relUtil * 0.25;
			}
		}
		
		return propensityUtil / numRel;
	}
	
}
