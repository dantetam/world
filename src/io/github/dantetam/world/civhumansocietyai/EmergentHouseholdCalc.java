package io.github.dantetam.world.civhumansocietyai;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.dantetam.world.civhumanrelation.HumanHumanRel;
import io.github.dantetam.world.civilization.Household;
import io.github.dantetam.world.life.Ethos;
import io.github.dantetam.world.life.Human;

/**
 * Utilities for calculating the potential split of a household into multiple households (strictly 2?)
 * @author Dante
 *
 */

public class EmergentHouseholdCalc {

	//Calculate the propensity of a split for independence, rebellion, other reasons besides marriage
	/**
	 * @param house The house in question
	 * @return The human that may want to leave, that we are inspecting
	 */
	public static Human calcBestHouseholdSplit(Household household, Date date) {
		Human bestHuman = null;
		double bestScore = 2.5;
		
		bestScore -= Math.pow(1.1, household.householdMembers.size());
		
		//For not so cordial breaks, disagreement with family/household
		for (Human candidate: household.householdMembers) {
			if (candidate.equals(household.headOfHousehold)) continue;
			double avgProsToLeave = PropensityUtil.calcPropensityToLeave(candidate, household.householdMembers, date, -25);
			if (avgProsToLeave > bestScore) {
				bestScore = avgProsToLeave;
				bestHuman = candidate;
			}
		}
		
		if (bestHuman == null) {
			for (Human candidate: household.householdMembers) {
				Ethos ethosIndep = candidate.brain.ethosPersonalityTraits.get("Independence");
				double severity = ethosIndep != null ? Math.log(ethosIndep.severity) : 0;
				if (severity > bestScore) {
					bestScore = severity;
					bestHuman = candidate;
				}
			}
		}
		
		return bestHuman;
	}
	
	/**
	 * @return The split of the household when certain people leave, into two groups:
	 * those who stay and those who leave, respectively.
	 */
	public static List<List<Human>> divideHouseholdsBySeparating(final Household house, 
			List<Human> separatingHumans, Date date) {
		List<Human> humansToMove = new ArrayList<>();
		humansToMove.addAll(separatingHumans);

		List<Human> humansStaying = new ArrayList<>();
		
		for (Human candidate: house.householdMembers) {
			double avgProsToLeave = PropensityUtil.calcPropensityToLeave(candidate, separatingHumans, date, 50);
			double avgProsToStay = PropensityUtil.calcPropensityToLeave(candidate, new ArrayList<Human>() {{add(house.headOfHousehold);}}, date, 50);
			if (avgProsToLeave > avgProsToStay * 1.2) {
				humansToMove.add(candidate);
			}
			else {
				humansStaying.add(candidate);
			}
		}
		
		return new ArrayList<List<Human>>() {{add(humansStaying); add(humansToMove);}};
	}
	
}
