package io.github.dantetam.world.civhumansocietyai;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.dantetam.toolbox.MapUtil;
import io.github.dantetam.world.civhumanrelation.HumanHumanRel;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.civilization.Society;
import io.github.dantetam.world.grid.GridRectInterval;
import io.github.dantetam.world.grid.LocalGrid;
import io.github.dantetam.world.life.Human;
import io.github.dantetam.world.process.LocalJob;
import io.github.dantetam.world.process.LocalProcess;

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
			return PropensityUtil.nonlinearRelUtil((oneWayRel.opinion - (-30)) / 30);
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
				if (i <= j) continue;
				Human otherHuman = humans.get(j);
				if (human.equals(otherHuman)) {
					continue;
				}
				double prosA = calcPropensityToMarry(human, otherHuman, date);
				double prosB = calcPropensityToMarry(human, otherHuman, date);
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
	
	public static Map<Human, GridRectInterval> possibleNewLandClaims(LocalGrid grid, List<Human> humans) {
		TODO
	}
	
	/**
	 * 
	 * @param host    The society that is considering raiding/attacking other societies for war or plunder
	 * @param humans  Humans in question from the host society
	 * @return The possible raid party (groups of humans) for use in free actions
	 */
	public static List<Human[]> possibleRaidingParties(Society host, List<Human> humans) {
		TODO
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
		return emplBossRelUtil;
	}
	
}
