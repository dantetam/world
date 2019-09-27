package io.github.dantetam.world.dataparse;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.distribution.GeometricDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

import io.github.dantetam.world.civilization.Skill;
import io.github.dantetam.world.civilization.SkillBook;

TODO;
//Fix skillbook data, abbreviations, and associations with certain processes, other human activities.

public class SkillData {

	private static Map<String, Skill> allSkills = new HashMap<>();
	
	/**
	 * For info access only. Use clones whenever possible
	 */
	public static Map<String, Skill> getAllSkills() {
		return allSkills;
	}

	public static void addSkill(String name, boolean isCoreSkill) {
		Skill newSkill = new Skill(name, SkillBook.MIN_LEVEL, isCoreSkill);
		newSkill.allExp = 0;
		allSkills.put(name, newSkill);
	}
	
	public static double leadershipCapPeople(int leadLevel) {
		return Math.pow(leadLevel, 1.6) * 1.4 + 10;
	}
	
	public static double rhetoricArgumentScore(int rhetoricLevel) {
		double mean = 0.25 + rhetoricLevel / SkillBook.MAX_LEVEL;
		double sd = mean * 0.2;
		double base = new NormalDistribution(mean, sd).sample();
		
		double probSmallAdj = new GeometricDistribution(0.9).sample(); 
		
		double geoDistSkill = 0.6 + 0.3 * rhetoricLevel / SkillBook.MAX_LEVEL;
		double probLargeAdj = new GeometricDistribution(geoDistSkill).sample();
		
		double argumentScore = base + probSmallAdj + probLargeAdj;
		return argumentScore;
	}
	
}
