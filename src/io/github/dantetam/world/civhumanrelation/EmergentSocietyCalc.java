package io.github.dantetam.world.civhumanrelation;

import java.util.List;

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
 * @author Dante
 *
 */

public class EmergentSocietyCalc {
	
	private static double nonlinearRelUtil(double rel) {
		return Math.signum(rel) * Math.abs(Math.pow(rel, 1.6));
	}
	
	public double calcPropensityHarmonySociety(List<Human> humans) {
		double propensityUtil = 0;
		double numRel = humans.size() * (humans.size() - 1) / 2;
		for (Human human: humans) {
			for (Human otherHuman: humans) {
				if (human.equals(otherHuman)) {
					HumanHumanRel oneWayRel = human.brain.getHumanRel(otherHuman);
					if (oneWayRel != null) {
						propensityUtil += nonlinearRelUtil(oneWayRel.opinion) / numRel;
					}
				}
			}
		}
		return propensityUtil;
	}
	
	public double calcPropensityTakeoverSociety(List<Human> humans) {
		
	}
	
	public double calcProsWealthPowerSociety(List<Human> humans) {
		
	}
	
}
