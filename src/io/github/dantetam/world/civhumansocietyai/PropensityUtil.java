package io.github.dantetam.world.civhumansocietyai;

import java.util.Date;
import java.util.List;

import io.github.dantetam.world.civhumanrelation.HumanHumanRel;
import io.github.dantetam.world.life.Human;

public class PropensityUtil {

	public static double nonlinearRelUtil(double rel) {
		return Math.signum(rel) * Math.pow(Math.abs(rel), 1.6);
	}
	
	//scale can be positive and negative. scale at 50 means that each 50 units of positive or negative
	//relation count for one quadratic-ish utility of 1. (1.6, 1.6^2, etc.)
	public static double calcPropensityToLeave(Human human, List<Human> humans, 
			Date date, double scale) {
		double avg = 0;
		if (humans.size() == 0) return 0;
		for (Human otherHuman: humans) {
			HumanHumanRel oneWayRel = human.brain.getHumanRel(otherHuman);
			if (oneWayRel != null) {
				oneWayRel.reevaluateOpinion(date);
				double opinion = oneWayRel.opinion / scale;
				avg += PropensityUtil.nonlinearRelUtil(opinion);
			}
		}
		return avg / humans.size();
	}
	
}
